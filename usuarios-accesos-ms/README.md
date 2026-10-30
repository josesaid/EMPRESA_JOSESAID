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
| Puerto | 8081 interno. No se publica en el host; se accede por el gateway (8080) |

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
# El gateway bloquea /actuator (404). Desde dentro de empresa-net:
curl -s http://usuarios-accesos-ms:8081/actuator/health
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
curl -s -X POST http://localhost:8080/usuarios-accesos-ms/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"correo":"admin@empresa.mx","password":"tu-clave"}'
```

**Ejemplo de alta (requiere token de ADMIN):**
```bash
curl -s -X POST http://localhost:8080/usuarios-accesos-ms/v1/usuarios \
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
- Refresh de token y cierre de sesión: implementado (ver sección Sesiones).
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


## Rotación de la clave RS256

usuarios-accesos-ms firma con una clave RS256 identificada por `kid` (por defecto `empresa-usuarios-1`). La clave pública se publica en `/.well-known/jwks.json`. Los servicios validan por `kid` y recargan el JWKS cuando reciben un `kid` que no conocen, así que no hace falta reiniciarlos.

**Procedimiento sin cortar sesiones (dos reinicios de usuarios-accesos-ms):**

1. Obtener la parte pública de la clave actual, en X.509 Base64. Ese valor va en `USUARIOS_JWT_CLAVE_PUBLICA_ANTERIOR`, y el `kid` actual en `USUARIOS_JWT_KID_ANTERIOR`:
   `echo "$USUARIOS_JWT_CLAVE_PRIVADA" | base64 -d | openssl pkey -inform DER -pubout -outform DER | base64`
2. Generar la clave nueva en PKCS#8 DER Base64 y guardarla en el gestor de secretos, nunca en el repositorio:
   `openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -outform DER | base64`
3. En el `.env` de la raíz: `USUARIOS_JWT_CLAVE_PRIVADA` con la clave nueva, `USUARIOS_JWT_KID` con un valor nuevo (por ejemplo `empresa-usuarios-2`), y `USUARIOS_JWT_CLAVE_PUBLICA_ANTERIOR` y `USUARIOS_JWT_KID_ANTERIOR` con los valores del paso 1.
4. Reiniciar: `docker compose up -d usuarios-accesos-ms`. Desde aquí el JWKS publica las dos claves y los tokens nuevos se firman con la clave nueva.
5. Esperar al menos 15 minutos (`JWT_DURACION`, por defecto 15 minutos) para que expiren los tokens firmados con la anterior.
6. Quitar `USUARIOS_JWT_CLAVE_PUBLICA_ANTERIOR` y `USUARIOS_JWT_KID_ANTERIOR` del `.env` y reiniciar otra vez. El JWKS deja de publicar la clave anterior.

**Efecto:** durante los pasos 4 y 5 validan tanto los tokens anteriores como los nuevos, así que las sesiones no se cortan.

**Si la clave se expone:** no esperar. Rotar y quitar la anterior de inmediato; los tokens firmados con ella dejan de validarse al recargar el JWKS.

**Refresh tokens:** son opacos y no dependen de la clave RS256; no cambian con la rotación.

**Calendario:** por definir. Se recomienda rotar al menos una vez al año.

## Área por usuario y rol JEFE_AREA

Cada usuario puede tener un área (`area`, texto de hasta 40 caracteres, por ejemplo `VENTAS_ATENCION_CLIENTES`). El área se asigna con `PUT /v1/usuarios/{id}/area`, body `{"area": "...", "motivo": "..."}`, solo con rol ADMIN. El motivo es obligatorio.

- El rol `JEFE_AREA` exige área. No se puede asignar sin ella (422), y no se puede crear un usuario con ese rol directamente: primero se crea, luego se asigna el área y después el rol.
- El token incluye el claim `area` cuando el usuario tiene una. reportes-ejecutivos-ms lo usa para limitar la consulta de indicadores.
- Un usuario que ya tenía sesión abierta antes de este cambio no trae el claim hasta volver a iniciar sesión.
- El valor de `area` debe coincidir con los nombres del enum `Area` de reportes-ejecutivos-ms. Este servicio no lo valida; si no coincide, reportes responde 403.
- Migración `V2__area_jefe.sql`: agrega la columna `area` y amplía la restricción de roles con `JEFE_AREA`.
- `GET /v1/usuarios/{id}` devuelve el área del usuario.
- El valor de `area` se valida contra la lista de áreas de reportes; un valor desconocido responde 422.
- Cada cambio de área publica el evento `area-asignada` (área anterior, área nueva y motivo), igual que el cambio de roles.

## Sesiones: refresh token y cierre de sesión

El login devuelve `accessToken` (15 minutos) y `refreshToken` (opaco, 7 días desde su último uso).

- `POST /v1/auth/refresh` con `{"refreshToken": "..."}` devuelve un access token y un refresh token nuevos. El anterior queda invalidado.
- Reusar un refresh token ya rotado revoca toda la sesión: el token nuevo también deja de servir. Es la señal de que el token se copió.
- `POST /v1/auth/logout` con `{"refreshToken": "..."}` cierra esa sesión. Es público porque basta con tener el token. Responde 204.
- `POST /v1/auth/logout-todas` requiere access token y cierra todas las sesiones del usuario. Responde 204.
- `POST /v1/auth/cambiar-password-inicial` con `{"correo", "passwordActual", "passwordNueva"}` cambia la contraseña temporal por una definitiva (mínimo 12 caracteres). Es público y responde 204. Mientras la contraseña sea temporal, el login responde 403 `cambio-password-requerido`.
- Solo se guarda el hash SHA-256 del refresh token, en la tabla `refresh_tokens` (migración `V3__refresh_tokens.sql`).
- Bloqueo o baja: el refresh falla porque se vuelve a verificar la cuenta. El access token ya emitido sigue válido hasta 15 minutos.
- Al cerrar sesión, el access token de esa sesión también se revoca en todos los servicios. Ver «Revocación de access tokens» más abajo.


## Revocación de access tokens

Al cerrar sesión, el access token de esa sesión deja de servir en todos los servicios, no solo en usuarios-accesos-ms. El claim `sid` del token es la familia del refresh token, así que basta con revocar ese identificador.

- `POST /v1/auth/logout` revoca la sesión y escribe `sesion-cerrada` (`sid`, `usuarioId`, `ocurridoEn`) en el outbox, en la misma transacción.
- `POST /v1/auth/logout-todas` escribe `sesiones-usuario-cerradas` (`usuarioId`, `ocurridoEn`). Los tokens emitidos antes de ese instante dejan de servir; un login posterior sí funciona.
- Cada servicio mantiene su copia en memoria (`RevocacionSesiones`), alimentada por `ListenerRevocaciones` desde el topic `usuarios-accesos.eventos`. `ValidadorRevocacion` la consulta en cada petición, además de firma y vencimiento.
- La copia conserva cada revocación 20 minutos, la vida del access token más un margen. Pasado ese tiempo, ningún token revocado sigue vigente.

**Flujo al cerrar sesión**

![Cierre de sesión: outbox, Kafka y rechazo en el servicio de área](docs/diagramas/01-cierre-de-sesion.png)

**Validación en cada servicio**

![Orden de validación: firma, sid revocado y cierre de todas las sesiones](docs/diagramas/02-validacion-token.png)

**Cerrar todas las sesiones**

![Cierre de todas las sesiones del usuario](docs/diagramas/03-cerrar-todas.png)

**Ventana de consistencia**

![Ventana entre el logout y la llegada del evento al servicio](docs/diagramas/04-ventana-consistencia.png)

Límites conocidos:

- Entre el logout y la llegada del evento a un servicio hay una ventana de segundos en la que el token todavía se acepta (diagrama 4).
- Las copias viven en memoria. Al arrancar, cada servicio lee de nuevo el topic desde el inicio y reconstruye las revocaciones vigentes. Esto requiere que la retención del topic sea mayor a 20 minutos; la retención por defecto de Kafka es de 7 días.
- El gateway no revisa revocación; la rechaza el servicio destino.


## Contraseña temporal

Las cuentas dadas de alta por `POST /v1/usuarios` reciben una contraseña temporal. Mientras esté pendiente de cambio, `POST /v1/auth/login` responde `403` (`cambio-password-requerido`) y no emite tokens.

Para cambiarla: `POST /v1/auth/cambiar-password-inicial` con `correo`, `passwordActual` (la temporal) y `passwordNueva` (mínimo 12 caracteres, distinta a la temporal). Responde `204`; después el usuario inicia sesión normalmente. El endpoint es público y cuenta los intentos fallidos igual que el login.
