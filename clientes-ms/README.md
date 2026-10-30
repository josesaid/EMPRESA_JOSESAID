# clientes-ms

Microservicio de Clientes del área comercial. Gestiona prospectos, clientes, contactos, interacciones con clientes y consentimientos de protección de datos. Lo consumen Pedidos, Facturación y la interfaz de ventas.

| Dato | Valor |
|---|---|
| Artefacto Maven | `com.mx.josesaid.develop:clientes-ms:0.1.0` |
| Paquete base | `com.mx.josesaid.develop.clientes` |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Base de datos | PostgreSQL 17 con Flyway |
| Mensajería | Kafka, con patrón outbox |
| Seguridad | OAuth2 / JWT (resource server) |
| Puerto | 8080 en el contenedor, 8101 en el host |

Documentación: [diseño](docs/diseno.md) · [diagramas](docs/diagramas.md)

---

## 1. Estructura del repositorio

```
clientes-ms/
├── pom.xml                      Dependencias y plugins
├── Dockerfile                   Compilación con Maven y ejecución con JRE 21, usuario no root
├── docker-compose.yml           Servicio clientes-ms; se usa solo o incluido desde la raíz
├── .env.example                 Variables del servicio (copiar a .env)
├── .gitignore
├── README.md
├── docs/
│   ├── diseno.md                Diseño, endpoints, eventos, pruebas, despliegue
│   ├── diagramas.md               Índice de imágenes con su descripción
│   └── diagramas/               Imágenes PNG y su fuente Mermaid (.mmd)
└── src/
    ├── main/java/com/mx/josesaid/develop/clientes/
    │   ├── ClientesApplication.java
    │   ├── domain/              Entidades, enums y reglas de estado. Sin HTTP
    │   ├── application/         Casos de uso, comandos, duplicados, eventos, auditoría
    │   ├── infrastructure/
    │   │   ├── persistence/     Repositorios Spring Data JPA
    │   │   ├── crypto/          Cifrado AES-256-GCM e índices HMAC-SHA256
    │   │   └── outbox/          Tabla de eventos y publicador hacia Kafka
    │   ├── api/                 Controladores, DTOs, errores, enmascarado, presentación
    │   └── config/              Seguridad, reloj UTC, claves de cifrado
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__esquema_inicial.sql
    └── test/
        ├── java/                Pruebas unitarias (*Test) y de integración (*IT)
        └── resources/application-test.yml
```

Reglas de dependencia:

- `domain` no depende de `api`, de Spring Web ni de Kafka. Usa JPA y el paquete de cifrado porque las entidades son JPA.
- `application` no conoce DTOs HTTP; recibe comandos.
- `api` traduce HTTP a comandos y convierte fechas a la zona de presentación.
- `infrastructure` contiene todo lo que depende de tecnologías concretas.

---

## 2. Requisitos

| Herramienta | Versión mínima | Uso |
|---|---|---|
| JDK | 21 | Compilar y ejecutar |
| Maven | 3.9 | Construir y probar |
| Docker | 24 | Imagen y pruebas `*IT` |
| Docker Compose | 2.20 | Levantar el servicio |
| OpenSSL | cualquiera | Generar claves de cifrado |

---

## 3. Configuración

### 3.1 Variables de entorno

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/clientes` | URL JDBC de PostgreSQL |
| `DB_USUARIO` | No | `clientes` | Usuario de la base de datos |
| `DB_PASSWORD` | Sí | (vacío) | Contraseña del usuario. En Docker se toma de `CLIENTES_DB_PASSWORD` |
| `KAFKA_BOOTSTRAP` | No | `localhost:9092` | Servidores de arranque de Kafka. En Docker: `kafka:29092` |
| `EVENTOS_TOPICO` | No | `clientes.eventos.v1` | Tópico donde se publican los eventos |
| `JWT_ISSUER_URI` | Sí en producción | `http://localhost:9000` | Emisor de los JWT (Usuarios y Accesos) |
| `CRYPTO_CLAVE_CIFRADO` | Sí | (vacío) | Clave AES-256 en Base64, 32 bytes |
| `CRYPTO_CLAVE_INDICE` | Sí | (vacío) | Clave HMAC-SHA256 en Base64, 32 bytes, distinta de la anterior |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | No | `http://localhost:4318/v1/traces` | Destino de las trazas OTLP HTTP |

Si `CRYPTO_CLAVE_CIFRADO` o `CRYPTO_CLAVE_INDICE` faltan, o no miden 32 bytes al decodificarse, la aplicación no arranca. El mensaje indica la variable afectada.

Propiedades de `application.yml` que se pueden sobrescribir:

| Propiedad | Valor | Significado |
|---|---|---|
| `app.zona-presentacion` | `America/Mexico_City` | Zona en la que se devuelven las fechas |
| `app.eventos.intervalo-publicacion-ms` | `1000` | Cada cuánto revisa el outbox |
| `app.seguridad.claim-roles` | `roles` | Claim del JWT con los roles |
| `spring.data.web.pageable.default-page-size` | `20` | Tamaño de página por defecto |
| `spring.data.web.pageable.max-page-size` | `100` | Tamaño máximo de página |
| `server.port` | `8080` | Puerto HTTP interno |

### 3.2 Generar claves

```bash
openssl rand -base64 32   # CRYPTO_CLAVE_CIFRADO
openssl rand -base64 32   # CRYPTO_CLAVE_INDICE
```

Advertencias:

- Si se pierde `CRYPTO_CLAVE_CIFRADO`, los datos cifrados no se recuperan.
- Si cambia `CRYPTO_CLAVE_INDICE`, los índices existentes dejan de coincidir: las búsquedas por RFC, correo o nombre y la detección de duplicados fallan hasta reindexar.
- Genere una clave distinta por entorno. Guárdelas en el gestor de secretos, no en el repositorio.

---

## 4. Ejecución

### 4.1 Con el ecosistema completo (recomendado)

Desde la raíz `EMPRESA_JOSESAID`:

```bash
docker network create empresa-net    # solo la primera vez
docker compose up -d --build
```

La API queda en `http://localhost:8101`.

### 4.2 Solo este servicio

Requiere la infraestructura de la raíz ya levantada:

```bash
cp .env.example .env                 # complete los valores
docker compose up -d --build
```

### 4.3 Localmente, con Maven

Con PostgreSQL y Kafka accesibles en `localhost`:

```bash
export DB_PASSWORD=...
export CRYPTO_CLAVE_CIFRADO=...
export CRYPTO_CLAVE_INDICE=...
export JWT_ISSUER_URI=https://usuarios.empresa.mx
mvn spring-boot:run
```

Flyway aplica las migraciones pendientes al arrancar.

### 4.4 Verificar que está arriba

```bash
curl http://localhost:8101/actuator/health
```

Respuesta esperada: `{"status":"UP"}`.

---

## 5. Pruebas

```bash
mvn test                                # 13 pruebas unitarias; no requieren Docker
mvn verify                              # unitarias + integración; requiere Docker
mvn -Dit.test=FlujoClientesIT verify    # solo integración
```

| Tipo | Clase | Requiere Docker | Qué verifica |
|---|---|---|---|
| Unitaria | `MascaraTest` (3) | No | Enmascarado de RFC, correo y nombre |
| Unitaria | `EstatusProspectoTest` (3) | No | Transiciones de prospecto |
| Unitaria | `EstatusClienteTest` (2) | No | Transiciones de cliente; `BAJA` terminal |
| Unitaria | `ConsentimientoTest` (2) | No | Revocación idempotente; vigencia |
| Unitaria | `ProspectoTest` (3) | No | Alta con índices, conversión solo desde `CALIFICADO` |
| Integración | `FlujoClientesIT` (5) | Sí | Conversión completa, RFC duplicado con 409, `BAJA` no reactivable, control de acceso por rol |

Las pruebas de integración usan PostgreSQL 17 y Kafka `apache/kafka:4.0.0` con Testcontainers. Si el daemon de Docker no está disponible, fallan al arrancar el contenedor; no se omiten en silencio.

Las claves de `src/test/resources/application-test.yml` son solo para pruebas.

---

## 6. Base de datos

### 6.1 Esquema

Flyway ejecuta `src/main/resources/db/migration/` al arrancar. `V1__esquema_inicial.sql` crea:

| Tabla | Columnas cifradas | Columnas de búsqueda (HMAC) | Restricciones |
|---|---|---|---|
| `prospecto` | `nombre_cifrado`, `correo_cifrado`, `rfc_cifrado` | `nombre_hash`, `correo_hash`, `rfc_hash` | `CHECK` de estatus y fuente |
| `cliente` | `rfc_cifrado` | `rfc_hash` | Único parcial sobre `rfc_hash` donde `estatus <> 'BAJA'` |
| `contacto` | `nombre_cifrado`, `correo_cifrado` | `correo_hash` | Único parcial: un `principal` por cliente |
| `interaccion` | `resumen_cifrado` | | Índice por `cliente_id, fecha DESC` |
| `consentimiento` | | | `CHECK` de medio |
| `outbox_evento` | | | Índice parcial sobre pendientes |

`cliente.razon_social` no está cifrado: es nombre de empresa, no dato personal de persona física. Se busca por coincidencia parcial sin distinguir mayúsculas.

Diagrama de relaciones: [docs/diagramas.md](docs/diagramas.md).

### 6.2 Migraciones

- Cada cambio nuevo va en un archivo `V<n>__descripcion.sql`. No se editan migraciones ya aplicadas.
- Hibernate está en `ddl-auto: validate`: la aplicación no modifica el esquema.
- Los cambios deben ser compatibles hacia atrás: agregar columnas opcionales, no eliminar columnas en uso.

---

## 7. Máquinas de estado

### 7.1 Prospecto

```
NUEVO ──► CONTACTADO ──► CALIFICADO ──► CONVERTIDO (solo por conversión)
  │           │              │
  └───────────┴──────────────┴──► PERDIDO (desde cualquier estado abierto)
```

- Repetir el estatus actual responde `200` sin cambios.
- Pasar a `CONVERTIDO` por PATCH responde `409 transicion-invalida`. Use `POST /conversion`.
- `PERDIDO` y `CONVERTIDO` son terminales.

### 7.2 Cliente

```
ACTIVO ◄──► SUSPENDIDO
  │            │
  └────┬───────┘
       ▼
      BAJA   (terminal)
```

- Repetir el estatus actual responde `200` sin evento.
- Intentar salir de `BAJA` responde `409 transicion-invalida`.

---

## 8. Seguridad

### 8.1 Autenticación

El token va en `Authorization: Bearer <token>`. El servicio valida la firma y el emisor contra `JWT_ISSUER_URI`.

Requisitos del token:

- Claim `sub` con un UUID. Se usa como identificador del actor en interacciones y auditoría. Si no es UUID, la petición falla.
- Claim `roles` (lista) con uno o más valores de la tabla 8.2. Cada rol se convierte en `ROLE_<NOMBRE>`.

Ejemplo de payload:

```json
{
  "sub": "8f3c2a1e-6b4d-4e5f-9a7b-1c2d3e4f5a6b",
  "iss": "https://usuarios.empresa.mx",
  "roles": ["VENDEDOR"],
  "exp": 1793400000
}
```

Respuestas: token ausente o inválido → `401`. Rol insuficiente → `403`.

### 8.2 Roles

| Rol | Puede |
|---|---|
| `VENDEDOR` | Registrar y convertir prospectos, cambiar su estatus, buscar y consultar clientes, registrar y consultar interacciones |
| `GERENTE_VENTAS` | Consultar prospectos, clientes e interacciones; consultar métricas de conversión |
| `ADMIN_CLIENTES` | Dar de alta y actualizar clientes, cambiar su estatus, agregar contactos, convertir prospectos, buscar y consultar clientes |
| `RESPONSABLE_PROTECCION_DATOS` | Otorgar y revocar consentimientos |

### 8.3 Datos personales

| Dato | En la base | En respuestas | En logs y eventos |
|---|---|---|---|
| RFC | Cifrado y hash | Enmascarado: `IDE*******Y1` | No |
| Correo | Cifrado y hash | Enmascarado: `a***@dominio.mx` | No |
| Nombre (prospecto y contacto) | Cifrado; hash solo en prospecto | Enmascarado: `A***` | No |
| Razón social | Texto plano | Completa | No |
| Resumen de interacción | Cifrado | Completo | No |

Todas las respuestas enmascaran RFC, correo y nombre, incluidas las de roles administrativos. Mostrar el valor completo a un rol es una decisión pendiente (sección 15).

### 8.4 Auditoría

Cada consulta de cliente, prospecto, interacciones o búsqueda escribe una línea en el logger `auditoria.datos_personales` con operación, identificador del recurso y `sub` del actor. No incluye el contenido de los datos.

---

## 9. API

Todas las rutas empiezan con `/v1`. Los errores siguen RFC 9457 (`application/problem+json`).

### 9.1 Endpoints

| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/v1/prospectos` | VENDEDOR | 201 | 400, 409 `rfc-duplicado`, 409 `posible-duplicado` |
| GET | `/v1/prospectos/{id}` | VENDEDOR, GERENTE_VENTAS | 200 | 404 |
| PATCH | `/v1/prospectos/{id}/estatus` | VENDEDOR | 200 | 400, 404, 409 `transicion-invalida` |
| POST | `/v1/prospectos/{id}/conversion` | VENDEDOR, ADMIN_CLIENTES | 201 | 400, 404, 409 `rfc-duplicado`, 409 `transicion-invalida` |
| POST | `/v1/clientes` | ADMIN_CLIENTES | 201 | 400, 409 `rfc-duplicado`, 409 `posible-duplicado` |
| GET | `/v1/clientes?rfc=&nombre=&page=&size=` | VENDEDOR, GERENTE_VENTAS, ADMIN_CLIENTES | 200 | 422 sin `rfc` ni `nombre` |
| GET | `/v1/clientes/{id}` | VENDEDOR, GERENTE_VENTAS, ADMIN_CLIENTES | 200 | 404 |
| PATCH | `/v1/clientes/{id}` | ADMIN_CLIENTES | 200 | 400, 404 |
| PATCH | `/v1/clientes/{id}/estatus` | ADMIN_CLIENTES | 200 | 400, 404, 409 `transicion-invalida` |
| POST | `/v1/clientes/{id}/contactos` | ADMIN_CLIENTES | 201 | 400, 404 |
| POST | `/v1/clientes/{clienteId}/interacciones` | VENDEDOR | 201 | 400, 404 |
| GET | `/v1/clientes/{clienteId}/interacciones?page=&size=` | VENDEDOR, GERENTE_VENTAS | 200 | 404 |
| POST | `/v1/contactos/{contactoId}/consentimientos` | RESPONSABLE_PROTECCION_DATOS | 201 | 400, 404 |
| POST | `/v1/consentimientos/{id}/revocacion` | RESPONSABLE_PROTECCION_DATOS | 200 | 404 |
| GET | `/v1/metricas/conversion?mes=yyyy-MM` | GERENTE_VENTAS | 200 | 400 si el formato del mes es inválido |

### 9.2 Parámetros

- `confirmarDuplicado` (booleano, por defecto `false`): en `POST /v1/prospectos` y `POST /v1/clientes`. Confirma la alta cuando hay coincidencia parcial por correo, nombre o razón social. No aplica al RFC.
- `page` (por defecto `0`) y `size` (por defecto `20`, máximo `100`).
- Búsqueda de clientes: se ordena por razón social ascendente. Se requiere `rfc` o `nombre`. Si se envían ambos, se usa `rfc`. `nombre` busca coincidencias parciales sin distinguir mayúsculas.
- Interacciones: se ordenan por `fecha` descendente.
- `mes`: formato `yyyy-MM`, por ejemplo `2026-10`. Se cuentan los prospectos con `CONVERTIDO` cuya `fecha_conversion` cae en ese mes en `America/Mexico_City`.

### 9.3 Ejemplos

Use `$TOKEN` con un JWT válido y `BASE=http://localhost:8101`.

**Crear prospecto**

```bash
curl -X POST "$BASE/v1/prospectos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Ana López","correo":"ana.lopez@ejemplo.mx","rfc":"LOPA800101AB1","fuente":"CAMPANA"}'
```

Respuesta `201`, cabecera `Location: /v1/prospectos/{id}`:

```json
{
  "id": "5b1e0c2a-1d2f-4a3b-9c8d-7e6f5a4b3c2d",
  "nombre": "A***",
  "correo": "a***@ejemplo.mx",
  "rfc": "LOP********B1",
  "fuente": "CAMPANA",
  "estatus": "NUEVO",
  "fechaRegistro": "2026-10-29",
  "fechaConversion": null
}
```

**Cambiar estatus de prospecto**

```bash
curl -X PATCH "$BASE/v1/prospectos/{id}/estatus" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"estatus":"CONTACTADO"}'
```

Respuesta `200` con el prospecto actualizado.

**Convertir prospecto**

El prospecto debe estar en `CALIFICADO`.

```bash
curl -X POST "$BASE/v1/prospectos/{id}/conversion" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
        "razonSocial": "Industrias Demo SA de CV",
        "rfc": "IDE900101XY1",
        "contactoPrincipal": {"nombre": "Luis Pérez", "correo": "luis@demo.mx", "rol": "COMPRAS"}
      }'
```

Respuesta `201`, cabecera `Location: /v1/clientes/{id}`:

```json
{
  "id": "3f1c2a9e-8b7d-4c1e-9a2f-5d6e7f8a9b0c",
  "razonSocial": "Industrias Demo SA de CV",
  "rfc": "IDE*******Y1",
  "estatus": "ACTIVO",
  "prospectoId": "5b1e0c2a-1d2f-4a3b-9c8d-7e6f5a4b3c2d",
  "fechaAlta": "2026-10-29"
}
```

Si el prospecto ya está `CONVERTIDO`, la operación devuelve el cliente existente con `201`.

**Crear cliente directamente**

```bash
curl -X POST "$BASE/v1/clientes?confirmarDuplicado=false" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
        "razonSocial": "Comercial Norte SA",
        "rfc": "CNO900101AB2",
        "contactoPrincipal": {"nombre": "Marta Ruiz", "correo": "marta@norte.mx", "rol": "PAGOS"}
      }'
```

Respuesta `201`. El contacto principal se crea siempre; en este alta no se puede omitir.

**Buscar clientes**

```bash
curl "$BASE/v1/clientes?nombre=comercial&page=0&size=20" -H "Authorization: Bearer $TOKEN"
```

Respuesta `200`, paginada con `content` y `page`. Cada elemento tiene `cliente` y `ultimaInteraccion` (o `null`).

**Cambiar estatus de cliente**

```bash
curl -X PATCH "$BASE/v1/clientes/{id}/estatus" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"estatus":"SUSPENDIDO"}'
```

**Agregar contacto**

```bash
curl -X POST "$BASE/v1/clientes/{id}/contactos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Jorge Soto","correo":"jorge@norte.mx","rol":"ASISTENCIA","principal":true}'
```

Si el contacto nuevo es `principal: true`, el anterior principal pasa a no principal en la misma transacción.

**Registrar interacción**

```bash
curl -X POST "$BASE/v1/clientes/{clienteId}/interacciones" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"LLAMADA","fecha":"2026-10-29T18:30:00Z","resumen":"Seguimiento de propuesta."}'
```

La fecha se envía en UTC. La respuesta la devuelve en `America/Mexico_City`: `"fecha":"2026-10-29T12:30:00-06:00"`.

**Historial de interacciones**

```bash
curl "$BASE/v1/clientes/{clienteId}/interacciones?page=0&size=20" -H "Authorization: Bearer $TOKEN"
```

**Otorgar consentimiento**

```bash
curl -X POST "$BASE/v1/contactos/{contactoId}/consentimientos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"finalidad":"Comunicaciones comerciales","fechaOtorgado":"2026-10-29T18:00:00Z","medio":"FORMULARIO","vigencia":"2027-10-29"}'
```

**Revocar consentimiento**

```bash
curl -X POST "$BASE/v1/consentimientos/{id}/revocacion" -H "Authorization: Bearer $TOKEN"
```

Revocar dos veces no emite dos eventos.

**Métricas de conversión**

```bash
curl "$BASE/v1/metricas/conversion?mes=2026-10" -H "Authorization: Bearer $TOKEN"
```

Respuesta: `{"mes":"2026-10","conversiones":12}`.

### 9.4 Errores

Todas las respuestas de error tienen `type`, `title`, `status` y `detail`. El `type` sigue el patrón `urn:clientes:error:<codigo>`.

| Código HTTP | `type` | Cuándo | Campos adicionales |
|---|---|---|---|
| 400 | `urn:clientes:error:validacion` | Campos inválidos | `errores`: lista de `{campo, mensaje}` |
| 400 | (Spring) | Formato inválido, por ejemplo `mes` | |
| 401 | (Spring) | Token ausente o inválido | |
| 403 | (Spring) | Rol insuficiente | |
| 404 | `urn:clientes:error:recurso-no-encontrado` | El identificador no existe | |
| 409 | `urn:clientes:error:rfc-duplicado` | Ya hay un cliente no dado de baja con ese RFC | `clienteExistenteId` |
| 409 | `urn:clientes:error:posible-duplicado` | Coincidencia de correo, nombre o razón social sin confirmar | `coincidencias`: lista de `{criterio, tipoRegistro, id}` |
| 409 | `urn:clientes:error:transicion-invalida` | Transición de estatus no permitida | |
| 409 | `urn:clientes:error:conflicto-de-integridad` | Violación de restricción única en concurrencia | |
| 422 | `urn:clientes:error:regla-de-negocio` | Búsqueda sin `rfc` ni `nombre` | |

Ejemplo `409 rfc-duplicado`:

```json
{
  "type": "urn:clientes:error:rfc-duplicado",
  "title": "Conflicto de RFC",
  "status": 409,
  "detail": "Ya existe un cliente activo con ese RFC",
  "clienteExistenteId": "3f1c2a9e-8b7d-4c1e-9a2f-5d6e7f8a9b0c"
}
```

---

## 10. Reglas de duplicados

| Situación | Resultado | Cómo se resuelve |
|---|---|---|
| RFC igual al de un cliente no dado de baja | `409 rfc-duplicado` | No se resuelve: la alta se rechaza. Aplica en prospectos, altas de cliente y conversiones |
| Correo igual al de un prospecto o contacto | `409 posible-duplicado` | Confirmar con `?confirmarDuplicado=true` |
| Nombre de prospecto igual, sin acentos ni mayúsculas | `409 posible-duplicado` | Confirmar con `?confirmarDuplicado=true` |
| Razón social igual a la de un cliente, sin mayúsculas | `409 posible-duplicado` | Confirmar con `?confirmarDuplicado=true` |

En la conversión solo aplica la regla de RFC; no hay confirmación de duplicados por correo o nombre.

Cada detección incrementa `clientes.duplicados.detectados` con la etiqueta `criterio` (`rfc` o `posible`).

Diagrama: [docs/diagramas/proceso-duplicados.png](docs/diagramas/proceso-duplicados.png).

---

## 11. Eventos

Los eventos se escriben en `outbox_evento` en la misma transacción del cambio de estado.

| Evento | Cuándo | Clave | Campos |
|---|---|---|---|
| `ClienteCreado` | Conversión de prospecto o alta directa de cliente | `clienteId` | `clienteId`, `contactoPrincipalId`, `estatus`, y `prospectoId` solo en conversión |
| `ClienteEstatusCambiado` | Cambio de estatus de cliente con valor distinto al anterior | `clienteId` | `clienteId`, `estatusAnterior`, `estatusNuevo` |
| `ConsentimientoRevocado` | Primera revocación de un consentimiento | `consentimientoId` | `consentimientoId`, `contactoId`, `finalidad` |

Todos los eventos incluyen `eventoId` (UUID), `tipoEvento` y `ocurridoEn` (instante UTC en ISO 8601). Ejemplo:

```json
{
  "eventoId": "0d9f3c1e-2b4a-4c6d-8e7f-1a2b3c4d5e6f",
  "tipoEvento": "ClienteEstatusCambiado",
  "ocurridoEn": "2026-10-29T18:30:00Z",
  "clienteId": "3f1c2a9e-8b7d-4c1e-9a2f-5d6e7f8a9b0c",
  "estatusAnterior": "ACTIVO",
  "estatusNuevo": "SUSPENDIDO"
}
```

Entrega:

- **Tópico:** valor de `EVENTOS_TOPICO`, por defecto `clientes.eventos.v1`.
- **Garantía:** al menos una vez. Los consumidores deben ser idempotentes usando `eventoId`.
- **Orden:** por agregado, siguiendo el orden de creación. El publicador se detiene en el primer fallo del lote.
- **Kafka caído:** los eventos quedan pendientes en la base y se publican al recuperarse.

Consulta de eventos pendientes:

```sql
SELECT tipo_evento, agregado_id, created_at
FROM outbox_evento
WHERE enviado_at IS NULL
ORDER BY created_at;
```

Diagramas: [docs/diagramas.md](docs/diagramas.md#eventos).

---

## 12. Outbox

- El publicador se ejecuta cada `app.eventos.intervalo-publicacion-ms` (1000 ms), con espera fija entre ciclos.
- Toma hasta 100 eventos pendientes por ciclo, en orden de creación.
- Cada envío espera confirmación de Kafka hasta 5 segundos.
- Ante el primer error o timeout, el ciclo termina y el resto se reintenta en el siguiente.
- Un evento se marca `enviado_at` solo cuando Kafka confirma.

Para revisar eventos atascados, use la consulta de la sección 11. Si un evento falla siempre, revise `KAFKA_BOOTSTRAP` y el tópico; no borre filas de `outbox_evento`.

---

## 13. Observabilidad

### 13.1 Endpoints de operación

| Ruta | Acceso | Contenido |
|---|---|---|
| `/actuator/health` | Público | Estado. Los detalles solo con autorización |
| `/actuator/info` | Público | Metadatos |
| `/actuator/prometheus` | Público | Métricas Prometheus. Exponer solo en la red interna |

### 13.2 Métricas

| Métrica | Etiquetas | Significado |
|---|---|---|
| `clientes.altas` | `tipo`: `prospecto` o `cliente` | Altas creadas |
| `clientes.conversiones` | | Conversiones de prospecto a cliente |
| `clientes.duplicados.detectados` | `criterio`: `rfc` o `posible` | Coincidencias que bloquearon una alta |
| `http.server.requests` | `uri`, `method`, `status`, `application` | Latencia y conteo por endpoint |

### 13.3 Logs y trazas

- Logs en JSON con formato ECS, en la salida estándar.
- Nunca se registran RFC, correo, nombre completo ni contenido de resúmenes.
- Trazas OpenTelemetry por OTLP HTTP a `OTEL_EXPORTER_OTLP_ENDPOINT`, con muestreo al 100%.

---

## 14. Despliegue y rollback

### 14.1 Despliegue

1. Confirmar la lista de verificación previa (abajo).
2. Desplegar con canary: 10% del tráfico de la interfaz de ventas.
3. Criterio de éxito: errores 5xx menores a 1% durante 30 minutos.
4. Si se cumple, ampliar gradualmente hasta 100%.

Lista de verificación previa:

- [ ] `CRYPTO_CLAVE_CIFRADO` y `CRYPTO_CLAVE_INDICE` están definidas en el entorno y son las de ese entorno.
- [ ] `JWT_ISSUER_URI` apunta al emisor correcto.
- [ ] El usuario `clientes` y su base existen en PostgreSQL.
- [ ] `KAFKA_BOOTSTRAP` responde y el tópico existe o el broker lo crea.
- [ ] `mvn verify` pasó en el pipeline, incluidas las pruebas `*IT`.

### 14.2 Rollback

- **Aplicación:** volver a la imagen anterior, `clientes-ms:<version-anterior>`.
- **Base de datos:** las migraciones no se revierten. Si hace falta corregir, se crea una nueva migración.
- **Operaciones incorrectas:** una alta o conversión errónea no se borra. Se compensa con una baja (`BAJA`), que deja el registro para auditoría.

---

## 15. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| Arranque falla con `Falta app.crypto.clave-cifrado` | `CRYPTO_CLAVE_CIFRADO` vacía | Definir la variable con 32 bytes en Base64 |
| `... debe ser de 32 bytes en Base64` | Clave con otra longitud | Regenerar con `openssl rand -base64 32` |
| `401` en todas las peticiones | `JWT_ISSUER_URI` no coincide con el claim `iss` | Revisar la variable y el token |
| `403` con token válido | El claim `roles` no tiene el rol requerido | Revisar la tabla 8.2 |
| `500` en peticiones con token válido | El claim `sub` no es un UUID | El emisor de tokens debe enviar UUID en `sub` |
| Búsqueda por RFC no encuentra clientes | `CRYPTO_CLAVE_INDICE` cambió después de crear los datos | Restaurar la clave original o reindexar |
| Eventos no llegan a Kafka | Broker caído o `KAFKA_BOOTSTRAP` incorrecto | Revisar la variable; los eventos se publican al recuperarse |
| Flyway falla al arrancar | Una migración aplicada fue modificada | No editar migraciones ya aplicadas; crear una nueva |
| `409 conflicto-de-integridad` en altas concurrentes | Dos peticiones con el mismo RFC al mismo tiempo | Esperado: solo una prospera. Consultar el cliente existente |
| `400` en `/v1/metricas/conversion` | `mes` con formato distinto de `yyyy-MM` | Use, por ejemplo, `2026-10` |
| Pruebas `*IT` fallan al arrancar | Docker no está en ejecución | Iniciar Docker y repetir |

---

## 16. Decisiones y pendientes

Decisiones tomadas:

- **Fuente `CAMPANA` sin tilde.** La especificación dice `CAMPAÑA`. Se usa sin tilde para evitar caracteres no ASCII en los valores guardados. Pendiente de confirmar.
- **Datos enmascarados en todas las respuestas,** incluidas las de roles administrativos.
- **Razón social sin cifrar,** por ser nombre de empresa.
- **Alta directa de cliente emite `ClienteCreado`,** igual que la conversión.
- **Errores con URN** (`urn:clientes:error:*`), no con una URL de dominio, para no depender de un dominio que aún no existe.

Pendientes (responsable: área de negocio y legal):

1. Confirmar `CAMPANA` o `CAMPAÑA`.
2. Definir si los roles administrativos ven RFC, correo y nombre completos.
3. Definir los valores definitivos de `MedioConsentimiento`. Actualmente: `FORMULARIO`, `CORREO`, `TELEFONO`, `PRESENCIAL`.
4. Definir el plazo de retención de prospectos no convertidos.
5. Revisar con el área legal el texto de consentimiento antes de producción.
6. Definir el procedimiento de anonimización para derechos ARCO.
7. Confirmar si `version` (concurrencia optimista) aplica también a clientes.

---

## 17. Referencias

- Diseño: [docs/diseno.md](docs/diseno.md)
- Diagramas: [docs/diagramas.md](docs/diagramas.md)
- Migraciones: `src/main/resources/db/migration/`
- Infraestructura compartida y base de datos: `README.md` de la raíz de `EMPRESA_JOSESAID`

## Descubrimiento, gateway y monitoreo

- Al arrancar, `clientes-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/clientes-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.
