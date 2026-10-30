# usuarios-accesos-ms

Servicio de identidad de EMPRESA_JOSESAID. Autentica usuarios, emite tokens JWT, administra roles y permisos, y registra eventos de seguridad.

## 1. Identidad
| Campo | Valor |
|---|---|
| Nombre | usuarios-accesos-ms |
| Artefacto Maven | com.mx.josesaid.develop:usuarios-accesos-ms:0.1.0-SNAPSHOT |
| Paquete base | com.mx.josesaid.develop.usuarios_accesos |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Base de datos | PostgreSQL 17 (esquema `usuarios_accesos`) |
| Mensajería | Kafka, tópico `usuarios-accesos.eventos` |
| Seguridad | OAuth2 resource server, JWT RS256 |
| Puerto | 8081 interno, 8102 en el host |

## 2. Propósito y alcance
- **Hace:** login, emisión de JWT, alta de usuarios, asignación de roles, baja, bloqueo por intentos fallidos.
- **No hace:** lógica de negocio de otras áreas, ni acceso de clientes externos.
- **Lo consumen:** todos los servicios de EMPRESA_JOSESAID, que validan el token que emite este servicio.

## 3. Estructura del repositorio
```
usuarios-accesos-ms/
├── pom.xml                  Dependencias y plugins Maven
├── Dockerfile               Imagen multietapa (Maven + JRE 21), usuario no root
├── docker-compose.yml       Servicio con comentarios; se incluye desde la raíz
├── .env.example             Variables necesarias (copiar a .env)
├── docs/diseno.md           Documento de diseño
└── src/main/java/.../usuarios_accesos/
    ├── domain/              Reglas de negocio, sin dependencias de Spring
    ├── application/         Casos de uso y puertos
    ├── infrastructure/      JPA, outbox, Kafka, JWT
    ├── api/                 Controladores REST y errores
    └── config/              Seguridad, reloj UTC, propiedades
```
**Reglas de dependencia:** `api` → `application` → `domain`. `infrastructure` implementa los puertos de `application` y `domain`. `domain` no importa nada de las otras capas.

## 4. Requisitos
- JDK 21 (verificado con 21.0.x).
- Maven 3.9 o superior.
- Docker 24 o superior y Docker Compose 2.20 o superior (para ejecutar con PostgreSQL y Kafka).

## 5. Configuración
| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| DB_URL | Sí | jdbc:postgresql://localhost:5432/usuarios_accesos | URL de PostgreSQL |
| DB_USER | Sí | usuarios_accesos | Usuario de base de datos |
| DB_PASSWORD | Sí | (vacío) | Contraseña de base de datos |
| KAFKA_BOOTSTRAP | No | localhost:9092 | Servidores de Kafka |
| `USUARIOS_JWT_CLAVE_PRIVADA` | Sí | Clave RSA privada (PKCS#8 en Base64) para firmar tokens |
| JWT_DURACION | No | PT15M | Vida del token (ISO-8601) |
| JWT_EMISOR | No | usuarios-accesos-ms | Claim `iss` |
| OUTBOX_INTERVALO_MS | No | 5000 | Frecuencia de envío de eventos pendientes |
| ADMIN_INICIAL_CORREO | No | (vacío) | Si se define, crea un ADMIN al arrancar si no existe |
| ADMIN_INICIAL_PASSWORD | No | (vacío) | Contraseña del administrador inicial. Cámbiala después |
| SERVER_PORT | No | 8081 | Puerto HTTP |

**Generar una clave JWT:** `openssl rand -base64 48`.

**Advertencia:** si cambias `JWT_CLAVE_PRIVADA`, los tokens emitidos quedan inválidos y la clave pública cambia en `/.well-known/jwks.json`. Al rotar, los servicios toman la nueva clave al reintentar la validación.

## 6. Ejecución
**Con todo el ecosistema (recomendado):** desde la raíz de EMPRESA_JOSESAID.
```bash
docker network create empresa-net   # solo la primera vez
docker compose up -d postgres kafka  # 1) infraestructura primero
docker compose up -d usuarios-accesos-ms  # 2) luego este servicio
```

**Orden de arranque:** PostgreSQL y Kafka antes que usuarios-accesos-ms. Si el servicio arranca antes de que la base esté lista, falla y se reintenta solo por `restart: unless-stopped`.

**Solo este servicio (con infraestructura ya levantada):**
```bash
docker compose up -d --build
```

**Localmente, con Maven** (requiere PostgreSQL y Kafka accesibles):
```bash
export DB_PASSWORD=... JWT_CLAVE_PRIVADA=...
mvn spring-boot:run
```

**Verificar salud:**
```bash
curl -s http://localhost:8102/actuator/health
```

## 7. Modelo de datos
| Tabla | Columna | Tipo | Restricción | Notas |
|---|---|---|---|---|
| usuarios | id | UUID | PK | |
| usuarios | correo | VARCHAR(254) | UNIQUE, NOT NULL | Normalizado a minúsculas |
| usuarios | nombre_completo | VARCHAR(200) | NOT NULL | Enmascarado en respuestas |
| usuarios | password_hash | VARCHAR(100) | NOT NULL | BCrypt |
| usuarios | estatus | VARCHAR(20) | CHECK ACTIVO, BLOQUEADO, BAJA | |
| usuarios | intentos_fallidos | INTEGER | CHECK >= 0 | |
| usuarios | bloqueado_hasta | TIMESTAMPTZ | | |
| usuarios | version | BIGINT | | Concurrencia optimista |
| usuarios | creado_utc, actualizado_utc | TIMESTAMPTZ | NOT NULL | UTC |
| usuario_roles | (usuario_id, rol) | UUID, VARCHAR(30) | PK, FK a usuarios | CHECK sobre los 10 roles |
| outbox_eventos | id | UUID | PK | |
| outbox_eventos | payload | TEXT | NOT NULL | Sobre JSON del evento |
| outbox_eventos | publicado_utc | TIMESTAMPTZ | | NULL = pendiente |

Índice parcial `idx_outbox_pendientes` sobre eventos pendientes.

**Columnas cifradas:** pendiente (ver sección 10).

## 8. Migraciones
- Flyway ejecuta los scripts de `src/main/resources/db/migration` al arrancar.
- Nombre: `V<n>__descripcion.sql`. Cada cambio es una migración nueva; no se editan las existentes.
- Hibernate solo valida el esquema (`ddl-auto: validate`).

## 9. Máquinas de estado
**Estatus del usuario:**
- `ACTIVO → BLOQUEADO` al quinto intento fallido. Vuelve a `ACTIVO` cuando vence el bloqueo o tras un login correcto.
- `ACTIVO` o `BLOQUEADO → BAJA` con el endpoint de baja.
- `BAJA` es final. Transiciones prohibidas: cualquier cambio desde `BAJA`.

**Idempotencia:** dar de baja un usuario ya dado de baja devuelve 422. Asignar los mismos roles de nuevo sí se acepta y genera un evento.

## 10. Seguridad
**Flujo:**
1. El cliente envía correo y contraseña a `POST /v1/auth/login`.
2. Si son correctos, recibe un JWT firmado con RS256 y válido por `JWT_DURACION`.
3. Cada petición posterior envía `Authorization: Bearer <token>`.

**Ejemplo de token (decodificado):**
```json
{ "iss": "usuarios-accesos-ms", "sub": "7f3c...", "roles": ["ADMIN"], "exp": 1761750000 }
```

**Roles:**

| Rol | Permisos en este servicio |
|---|---|
| ADMIN | Crear, consultar, asignar roles y dar de baja usuarios |
| Otros roles | Sin acceso a los endpoints de administración; su token es válido en otros servicios |

**Respuestas:**
- 401: token ausente, inválido o credenciales incorrectas.
- 403: token válido sin el rol requerido.

**Datos personales:** correo y nombre se enmascaran en las respuestas. Nunca se registran en logs.

**Pendiente:** cifrado AES-256-GCM en columnas (ver `docs/diseno.md`).

## 11. API
Documentación completa de endpoints en `docs/diseno.md`.

**Ejemplo de login:**
```bash
curl -s -X POST http://localhost:8102/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"correo":"admin@empresa.mx","password":"tu-clave"}'
```

**Ejemplo de alta (requiere token de ADMIN):**
```bash
curl -s -X POST http://localhost:8102/v1/usuarios \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"correo":"ana@empresa.mx","nombreCompleto":"Ana Ruiz","password":"clave-nueva-1","roles":["VENTAS"]}'
```

**Formato de error (RFC 9457):**
```json
{ "type": "urn:josesaid:usuarios-accesos:error:credenciales-invalidas", "title": "Unauthorized", "status": 401, "detail": "Correo o contraseña incorrectos" }
```
Tipos: `credenciales-invalidas` (401), `cuenta-bloqueada` (423), `no-encontrado` (404), `conflicto` (409), `regla-de-negocio` (422), `datos-invalidos` (400).

## 12. Reglas de duplicados
- Un correo no puede repetirse. Se compara normalizado a minúsculas.
- Si ya existe, la alta devuelve 409 con el mensaje "Ya existe un usuario con ese correo".

## 13. Eventos
| Evento | Cuándo | Tópico | Clave | Payload |
|---|---|---|---|---|
| usuario-bloqueado | Quinto intento fallido | usuarios-accesos.eventos | usuarioId | tipo, agregadoId, datos{usuarioId, intentosFallidos} |
| rol-asignado | Cambio de roles | usuarios-accesos.eventos | usuarioId | tipo, agregadoId, datos{usuarioId, rolesAnteriores, rolesNuevos, motivo} |

**Garantías:** al menos una vez (el consumidor debe ser idempotente). Orden por usuario, gracias a la clave.

**Si Kafka cae:** los eventos quedan en `outbox_eventos` y se envían cuando Kafka regresa.

**Ver pendientes:**
```sql
SELECT tipo, creado_utc, intentos FROM outbox_eventos WHERE publicado_utc IS NULL ORDER BY creado_utc;
```

## 14. Outbox
- Intervalo: `OUTBOX_INTERVALO_MS` (5 s por defecto).
- Lote: hasta 100 eventos por ciclo.
- Reintentos: el evento fallido se reintenta en el siguiente ciclo. El envío se detiene en el primer fallo para conservar el orden.
- Revisar eventos atascados: la consulta de la sección 13. Si `intentos` crece, revisar Kafka.

## 15. Observabilidad
**Endpoints de Actuator:** `/actuator/health` (público), `/actuator/info` y `/actuator/prometheus`.

**Logs:** JSON en formato ECS.

**Nunca se registra:** correo, nombre, contraseña, hash ni token.

**Métricas:** las de Micrometer por defecto (HTTP, JVM, HikariCP) y las de Spring Kafka. Métricas de negocio: pendientes.

**Trazas:** pendientes. Se integrarán con el servicio de trazas de la plataforma (ver pendientes generales).

## 16. Pruebas
| Comando | Qué ejecuta | Requiere Docker |
|---|---|---|
| `mvn -B test` | 17 pruebas unitarias de dominio y aplicación | No |
| `mvn -B verify` | Unitarias y `UsuariosAccesosIT` (PostgreSQL y Kafka reales) | Sí |

**Cobertura:**
- Unitarias: bloqueo, desbloqueo, baja, roles, normalización de correo, login correcto e incorrecto, eventos.
- Integración: login real contra PostgreSQL con Flyway, y ruta protegida sin token.

## 17. Despliegue
1. Construir la imagen: `docker compose build usuarios-accesos-ms`.
2. Levantar con la raíz: `docker compose up -d usuarios-accesos-ms`.
3. Verificar `/actuator/health`.
4. Criterio de éxito del canary: errores 5xx menores a 1% durante 30 minutos (anexo de la empresa).

Este servicio se despliega primero, porque emite los tokens que usan los demás.

## 18. Rollback
- Revertir la imagen a la versión anterior.
- Las migraciones de base de datos no se eliminan; se compensan con una nueva migración.
- Un cambio de roles o una baja incorrecta se corrige con una nueva asignación; no hay borrado físico.

## 19. Solución de problemas
| Síntoma | Causa probable | Solución |
|---|---|---|
| 401 en todas las rutas | No se puede leer `JWT_JWKS_URI` | Verifique que usuarios-accesos-ms esté arriba y que `/.well-known/jwks.json` responda |
| 401 con contraseña correcta | Cuenta bloqueada o dada de baja | Revisar `estatus` y `bloqueado_hasta` en la tabla usuarios |
| 423 Locked | Cinco intentos fallidos | Esperar 15 minutos o desbloquear desde la base de datos |
| 403 en endpoints de usuarios | El token no tiene rol ADMIN | Iniciar sesión con un usuario ADMIN |
| Flyway falla al arrancar | Migración con error o base de datos inconsistente | Revisar `flyway_schema_history` y la migración indicada en el log |
| Eventos pendientes no salen | Kafka no responde | Revisar `KAFKA_BOOTSTRAP` y el estado de Kafka |
| Error de conexión a PostgreSQL | Contraseña distinta a la de init-databases.sh | Alinear DB_PASSWORD con USUARIOS_ACCESOS_DB_PASSWORD |

## 20. Decisiones y pendientes
**Decisiones tomadas:**
- BCrypt para contraseñas. Motivo: disponible en Spring Security sin dependencias extra. Argon2 queda pendiente.
- Bloqueo de 15 minutos tras 5 intentos. Propuesta: pendiente de validación de seguridad.
- Administrador inicial por variables de entorno, para resolver el arranque en frío.

**Pendientes (responsable: equipo de identidad):**
- Refresh de token y cierre de sesión.
- Cifrado AES-256-GCM de columnas y HMAC para búsquedas.
- Idempotency-Key en escrituras.
- Métricas de negocio y trazas distribuidas.
- Registro en service discovery y API gateway (se configuran al final de la plataforma).

## 21. Referencias
- Diseño: `docs/diseno.md`.
- Especificación del área: `area_md_specification_files/Usuarios_Accesos.md`.
- Infraestructura compartida y README de la raíz: `../README.md`.

## Descubrimiento, gateway y monitoreo

- Al arrancar, `usuarios-accesos-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/usuarios-accesos-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Indicadores para reportes ejecutivos

Publica un indicador mensual al tópico `indicadores-areas`, que consume `reportes-ejecutivos-ms`.

- **Indicador:** `usuarios_activos` (área `ADMINISTRACION_LEGAL`).
- **Definición:** Usuarios activos: usuarios en estatus ACTIVO en este momento. Foto del mes actual.
- **Clave del mensaje:** `ADMINISTRACION_LEGAL|usuarios_activos|AAAA-MM`. Reportes actualiza el valor por área, nombre y periodo (upsert).
- **Ciclo:** cada 60 minutos (solo mes actual); el primer envío ocurre a los 30 segundos.
- **Configuración:** `INDICADORES_TOPICO`, `INDICADORES_INTERVALO_MS`, `INDICADORES_INICIO_MS`.
- **Entrega:** no pasa por el outbox. Es una foto; si un envío falla, el siguiente ciclo lo corrige.
- **Clase:** `PublicadorIndicadoresUsuarios`. Prueba: `mvn verify -Dit.test='PublicadorIndicadores*IT'`.
