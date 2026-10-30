# facturacion-pagos-ms

Microservicio del área **Facturación y pagos** de EMPRESA_JOSESAID. Emite comprobantes a partir de órdenes, registra cobros y gestiona cancelaciones.

## 1. Identidad

- **Servicio:** facturacion-pagos-ms · **Versión:** 0.1.0 · **Lenguaje:** Java 21 · **Framework:** Spring Boot 4.1.2
- **Puerto en tu Mac:** 8103 (contenedor: 8081) · **Base de datos:** `facturacion_pagos`
- **Responsable:** pendiente de asignar · **Estado:** emisión provisional, sin timbrado fiscal (ver sección 20)

## 2. Propósito y alcance

Crea comprobantes en borrador a partir de una orden, los emite con folio, registra pagos contra su saldo y los cancela con motivo.

**Dentro del alcance:** comprobantes, cálculo de IVA y total, pagos parciales o totales, cancelación con motivo, publicación de eventos.

**Fuera del alcance:** timbrado CFDI con el PAC, complementos de pago, contabilidad (lo hace contabilidad-ms), cobranza a clientes (la hace clientes-ms) y emisión de pedidos (la hace pedidos-ventas-ms).

## 3. Estructura del repositorio

```
facturacion-pagos-ms/
├── pom.xml
├── Dockerfile
├── docker-compose.yml          Servicio, con comentarios
├── .env.example                Variables requeridas (sin valores reales)
├── docs/diseno.md              Diseño técnico
├── src/main/java/.../facturacion_pagos/
│   ├── domain/                 Comprobante, Pago, reglas y puertos (sin Spring)
│   ├── application/            FacturacionService y publicador de eventos
│   ├── infrastructure/
│   │   ├── persistence/        Entidades JPA y adaptador del repositorio
│   │   └── outbox/             Outbox y retransmisor a Kafka
│   ├── api/                    Controlador REST y manejo de errores
│   └── config/                 Seguridad JWT, reloj y propiedades
└── src/test/java/...           Pruebas unitarias e integración
```

## 4. Requisitos

- JDK 21 y Maven 3.9 (solo para compilar fuera de Docker)
- Docker Desktop con Compose v2
- Red `empresa-net` creada una vez: `docker network create empresa-net`

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `FACTURACION_DB_PASSWORD` | Sí (Compose) | Contraseña del usuario `facturacion_pagos`. Va en el `.env` de la raíz o de esta carpeta. |
| `JWT_JWKS_URI` | No | URL del JWKS de usuarios-accesos-ms (clave pública). Ya viene configurada en el Compose |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | No | Datos de conexión. Compose ya los fija. |
| `KAFKA_BOOTSTRAP` | No | Por defecto `localhost:9092`. En Compose: `kafka:29092`. |
| `JWT_DURACION` | No | Vida del token (ISO-8601), solo para referencia. Por defecto `PT15M`. |
| `OUTBOX_INTERVALO_MS` | No | Cada cuántos milisegundos se envían eventos pendientes. Por defecto 5000. |

Ningún valor real está en el código. Las credenciales viven solo en `.env`, que está en `.gitignore`.

## 6. Ejecución

**Orden de arranque (obligatorio):**

1. Red: `docker network create empresa-net` (una sola vez)
2. Infraestructura de la raíz: `docker compose up -d postgres kafka` desde la raíz
3. **usuarios-accesos-ms** primero, porque emite los tokens que este servicio valida
4. **facturacion-pagos-ms** (este servicio)

Este servicio puede arrancar solo, pero sin PostgreSQL y Kafka no inicia. Para levantarlo:

```bash
cd facturacion-pagos-ms
docker compose up -d --build
curl -s http://localhost:8103/actuator/health
```

Para desarrollo sin contenedor: `mvn spring-boot:run`, con las variables de la sección 5 y PostgreSQL y Kafka accesibles en localhost.

## 7. Modelo de datos

- **comprobantes:** id, orden_id, folio (único), moneda (MXN o USD), subtotal, iva, total, tasa_iva, estatus, motivo_cancelacion, version, creado_utc, actualizado_utc.
- **pagos:** id, comprobante_id (FK), monto, fecha_pago, referencia, creado_utc. Los pagos no se modifican ni se borran.
- **outbox_eventos:** id, tipo, agregado_id, payload, creado_utc, publicado_utc, intentos.

Todos los importes son `NUMERIC(14,2)`. Las fechas de sistema son `TIMESTAMPTZ` en UTC.

## 8. Migraciones

- Flyway ejecuta `V1__esquema_inicial.sql` al arrancar.
- Hibernate solo valida el esquema (`ddl-auto: validate`).
- Las migraciones son hacia adelante: cambios nuevos en `V2`, `V3`… Nunca se editan migraciones ya aplicadas.

## 9. Máquinas de estado

**Comprobante:** `BORRADOR → EMITIDO → CANCELADO`.
- Solo un borrador se emite, y al emitir se asigna el folio.
- Solo un emitido se cancela, y la cancelación exige motivo.
- Un cancelado no vuelve a emitirse.

**Cobro** (derivado, no se guarda): `PENDIENTE` (sin pagos) → `PARCIAL` → `PAGADO` (saldo en cero).

Los pagos solo se aceptan en comprobantes emitidos y nunca exceden el saldo.

## 10. Seguridad

- API sin estado con JWT RS256 (resource server). Solo `/actuator/health` es público.
- Roles por método con `@PreAuthorize`:
  - `FINANZAS`: crear, emitir, cancelar y registrar pagos. Además leer.
  - `VENTAS`: solo leer.
- Sin `ROLE_FINANZAS` ni `ROLE_VENTAS` la respuesta es 403; sin token, 401.
- El servicio falla al arrancar si el clave pública JWKS tiene menos de 32 bytes.

## 11. API

Base: `/v1/comprobantes`

| Método | Ruta | Rol | Respuesta |
|---|---|---|---|
| POST | `/` | FINANZAS | 201 comprobante en borrador |
| GET | `/{id}` | FINANZAS, VENTAS | 200 comprobante con pagos y saldo |
| POST | `/{id}/emision` | FINANZAS | 200 comprobante emitido con folio |
| POST | `/{id}/cancelacion` | FINANZAS | 200 comprobante cancelado |
| POST | `/{id}/pagos` | FINANZAS | 201 comprobante actualizado |

Ejemplo de creación:

```json
{ "ordenId": "00000000-0000-0000-0000-000000000001", "moneda": "MXN", "subtotal": 1000, "tasaIva": 0.16 }
```

Los errores siguen RFC 9457 (`ProblemDetail`) con `type` `urn:josesaid:facturacion-pagos:error:<slug>`: `datos-invalidos` (400), `no-encontrado` (404), `conflicto` (409) y `regla-de-negocio` (422).

## 12. Reglas de duplicados

- Folio: único por base de datos. El folio es provisional y se genera en la emisión.
- Pagos: no hay deduplicación todavía. Dos envíos iguales registran dos pagos. Pendiente `Idempotency-Key` (sección 20).

## 13. Eventos

Se publican en el tópico `facturacion-pagos.eventos`, con la clave `agregadoId` (id del comprobante).

| Tipo | Cuándo | Datos |
|---|---|---|
| `comprobante-emitido` | Al emitir | comprobanteId, folio, total |
| `pago-registrado` | Al registrar un pago | comprobanteId, monto, saldo |
| `comprobante-cancelado` | Al cancelar | comprobanteId, motivo |

## 14. Outbox

El evento se guarda en `outbox_eventos` dentro de la misma transacción que el cambio de negocio. Así no se pierde un evento si Kafka no responde. El retransmisor lo envía cada `OUTBOX_INTERVALO_MS`, en orden de creación, y se detiene en el primer fallo para no romper el orden.

## 15. Observabilidad

- Salud: `GET /actuator/health`
- Métricas: `GET /actuator/prometheus`
- Logs JSON en formato ECS para el anexo de la empresa.
- Pendiente: trazas distribuidas y métricas de negocio (sección 20).

## 16. Pruebas

- Unitarias (9): reglas de `Comprobante` y `FacturacionService` con repositorio en memoria.
- Integración (1): PostgreSQL 17 y Kafka 4.0.0 reales con Testcontainers. Cubre roles, errores y el flujo completo.

```bash
mvn -B verify          # unitarias e integración, requiere Docker
mvn -B package -DskipITs   # solo unitarias
```

Para verificar en tu Mac: `bash ejecutar-pruebas-facturacion.sh` desde la raíz del repositorio.

## 17. Despliegue

Imagen multi-etapa (Maven para compilar, JRE 21 para ejecutar), usuario sin privilegios (uid 10001) y `HEALTHCHECK` sobre `/actuator/health`. Se despliega con `docker compose up -d --build` o publicando la imagen `facturacion-pagos-ms:0.1.0`.

## 18. Rollback

- Las migraciones no tienen rollback automático. Revertir código no revierte el esquema.
- Para volver a una versión anterior: desplegar la imagen anterior, siempre que sea compatible con el esquema actual.
- Un cambio de esquema que rompa compatibilidad requiere una migración nueva que corrija los datos, no borrarlos.

## 19. Solución de problemas

- **401 en todas las rutas:** `JWT_JWKS_URI` no responde; revise que usuarios-accesos-ms esté arriba.
- **"Defina FACTURACION_DB_PASSWORD":** falta la variable en `.env`. Compose se detiene a propósito.
- **Todas las peticiones dan 401:** el secreto no coincide con el de usuarios-accesos-ms, o el token expiró.
- **Error de conexión a la base:** PostgreSQL no está arriba o `init-databases.sh` no creó el usuario. Revisa que se haya creado el volumen con la variable definida.

## 20. Decisiones y pendientes

- **Decisión:** el folio es provisional (`FAC-` + 8 caracteres). El folio fiscal vendrá del PAC.
- **Pendiente crítico:** timbrado CFDI con PAC, folio y UUID fiscal.
- **Pendiente:** complementos de pago y cancelación ante el SAT.
- **Pendiente:** `Idempotency-Key` en pagos y creación.
- **Pendiente:** trazas distribuidas y métricas de negocio.
- **Pendiente:** diagramas en PNG (`docs/diagramas`) y reglas de tasas de IVA por tipo de operación.

## 21. Referencias

- Especificación del área: `area_md_specification_files/Facturacion_Pagos.md`
- Diseño técnico: `docs/diseno.md`
- Documento de arranque de la raíz: `README.md` del repositorio (sección de orden de arranque)

## Descubrimiento, gateway y monitoreo

- Al arrancar, `facturacion-pagos-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/facturacion-pagos-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Idempotencia de altas (`Idempotency-Key`)

El alta de `POST /v1/comprobantes/{id}/pagos` acepta la cabecera opcional `Idempotency-Key`. Úsela cuando un reintento no debe crear el registro dos veces (por ejemplo, si se cortó la conexión y no sabe si la petición llegó).

- **Misma clave y mismo cuerpo:** responde la misma respuesta original; no se crea nada nuevo.
- **Misma clave con otro cuerpo:** responde `422` (`regla-de-negocio`).
- **Misma clave mientras la primera petición sigue en proceso:** responde `409` (`conflicto-de-estado`).
- **Sin la cabecera:** la petición se procesa como siempre.

Las claves se guardan 24 horas en la tabla `idempotencia`. Si la operación falla, la clave se libera para que el cliente pueda reintentar.
