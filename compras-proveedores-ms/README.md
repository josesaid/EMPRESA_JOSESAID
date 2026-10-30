# compras-proveedores-ms

Microservicio del área **Compras y proveedores**. Administra proveedores con datos fiscales cifrados, órdenes de compra, su aprobación, la recepción de insumos y su cancelación.

## 1. Identidad

- **Servicio:** compras-proveedores-ms · **Versión:** 0.1.0 · **Lenguaje:** Java 21 · **Framework:** Spring Boot 4.1.2
- **Acceso:** vía gateway, sin puerto publicado (contenedor: contenedor: 8081) · **Base de datos:** `compras`
- **Responsable:** pendiente de asignar · **Estado:** funcional; monto límite de aprobación pendiente de confirmar (sección 20)

## 2. Propósito y alcance

Da de alta proveedores validando su RFC, emite órdenes de compra que DIRECCION aprueba, registra recepciones parciales o totales y cancela órdenes con motivo.

**Dentro del alcance:** proveedores, órdenes de compra, aprobación, recepciones, cancelaciones y eventos de recepción.

**Fuera del alcance:** existencias (inventario-operaciones-ms), pagos a proveedores (facturacion-pagos-ms, pendiente) y asientos contables (contabilidad-ms).

## 3. Estructura del repositorio

```
compras-proveedores-ms/
├── pom.xml
├── Dockerfile
├── docker-compose.yml          Servicio, con comentarios
├── .env.example                Variables requeridas (sin valores reales)
├── docs/diseno.md              Diseño técnico
├── src/main/java/.../compras/
│   ├── domain/                 Proveedor, OrdenCompra, LineaOrden, Recepcion y puertos
│   ├── application/            ComprasService y publicador de eventos
│   ├── infrastructure/
│   │   ├── persistence/        Entidades JPA con datos cifrados
│   │   ├── seguridad/          ProtectorDatos: AES-256-GCM e índice HMAC
│   │   └── outbox/             Outbox y retransmisor a Kafka
│   ├── api/                    Controlador REST, enmascarado y errores
│   └── config/                 Seguridad JWT, reloj y propiedades
└── src/test/java/...           Pruebas unitarias e integración
```

## 4. Requisitos

- JDK 21 y Maven 3.9 (solo para compilar fuera de Docker)
- Docker Desktop con Compose v2
- Red `empresa-net`: `docker network create empresa-net` (una sola vez)

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `COMPRAS_DB_PASSWORD` | Sí (Compose) | Contraseña del usuario `compras` |
| `JWT_JWKS_URI` | No | URL del JWKS de usuarios-accesos-ms (clave pública). Ya viene configurada en el Compose |
| `COMPRAS_CIFRADO_CLAVE` | Sí (Compose) | Clave AES-256 en base64 (32 bytes). Si se pierde, los RFC cifrados no se recuperan. |
| `COMPRAS_INDICE_CLAVE` | Sí (Compose) | Clave HMAC en base64 para buscar RFC. Si cambia, la búsqueda por RFC deja de encontrar proveedores existentes. |

Las claves de cifrado nunca se escriben en el código ni en el repositorio. Guárdalas en `.env` y respáldalas fuera de la máquina.

## 6. Ejecución

**Orden de arranque (obligatorio):**

1. Red: `docker network create empresa-net` (una sola vez)
2. Raíz: `docker compose up -d postgres kafka`
3. **usuarios-accesos-ms** primero, porque emite los tokens que este servicio valida
4. **compras-proveedores-ms** (este servicio). Puede arrancar en cualquier orden respecto de los demás servicios de negocio.

```bash
cd compras-proveedores-ms
docker compose up -d --build
curl -s http://localhost:8080/compras-proveedores-ms/actuator/health
```

## 7. Modelo de datos

- **proveedores:** id, razon_social, rfc_cifrado, rfc_indice (único), correo_cifrado, estatus, version.
- **ordenes_compra:** id, proveedor_id, moneda, estatus, motivo_cancelacion, version.
- **lineas_orden:** id, orden_id, articulo_sku, cantidad_ordenada, cantidad_recibida, precio_unitario. Única por orden y SKU.
- **recepciones:** id, orden_id, articulo_sku, cantidad, fecha. Historial inmutable.
- **outbox_eventos:** id, tipo, agregado_id, payload, creado_utc, publicado_utc, intentos.

El total de una orden se calcula desde sus líneas; no se almacena.

## 8. Migraciones

Flyway ejecuta `V1__esquema_inicial.sql`. Hibernate solo valida. Los cambios van en `V2`, `V3`…

## 9. Máquinas de estado

**Orden de compra:**
- `BORRADOR → APROBADA` (solo DIRECCION).
- `APROBADA → RECIBIDA_PARCIAL → RECIBIDA` según las recepciones.
- `BORRADOR` o `APROBADA → CANCELADA` con motivo.

Una orden aprobada no se modifica. Una orden recibida o cancelada no admite más cambios.

**Proveedor:** `ACTIVO`, `INACTIVO` o `BLOQUEADO`. Solo un proveedor activo recibe órdenes nuevas.

## 10. Seguridad

- JWT RS256 emitido por usuarios-accesos-ms. Solo `/actuator/health` es público.
- **COMPRAS:** alta de proveedores, órdenes y cancelaciones. Consulta órdenes.
- **DIRECCION:** aprueba órdenes. Consulta órdenes.
- **ALMACEN:** registra recepciones. Consulta órdenes.
- **FINANZAS:** consulta proveedores.
- **Datos sensibles:** RFC y correo se cifran con AES-256-GCM. En las respuestas el RFC se enmascara (`AAN*******B3`) y el correo muestra solo la primera letra y el dominio.

## 11. API

Base: `/v1`

| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/proveedores` | COMPRAS | 201 | 400, 409 (RFC duplicado) |
| GET | `/proveedores/{id}` | COMPRAS, FINANZAS | 200 | 404 |
| POST | `/ordenes-compra` | COMPRAS | 201 | 400, 404, 422 (proveedor inactivo) |
| GET | `/ordenes-compra/{id}` (añadido) | COMPRAS, ALMACEN, DIRECCION | 200 | 404 |
| POST | `/ordenes-compra/{id}/aprobacion` | DIRECCION | 200 | 404, 409 |
| POST | `/ordenes-compra/{id}/recepciones` | ALMACEN | 201 | 400, 404, 409 (excede lo ordenado), 422 (SKU ajeno) |
| POST | `/ordenes-compra/{id}/cancelacion` | COMPRAS | 200 | 400 (sin motivo), 404, 409 |

Ejemplo de recepción parcial:

```json
{ "lineas": [ { "articuloSku": "TORN-1", "cantidad": 4 } ] }
```

Errores RFC 9457 con `type` `urn:josesaid:compras-proveedores:error:<slug>`.

## 12. Reglas de duplicados

- RFC único: se compara por el índice HMAC del RFC normalizado (mayúsculas y sin espacios). Un duplicado responde 409.
- Un SKU aparece una sola vez por orden.
- Una recepción repetida registra otra recepción mientras no exceda lo ordenado. Pendiente `Idempotency-Key`.

## 13. Eventos

Tópico `compras.eventos`, clave = id de la orden.

| Tipo | Cuándo | Datos |
|---|---|---|
| `orden-compra-aprobada` | Al aprobar | id, proveedor_id, total |
| `insumos-recibidos` | Al registrar una recepción | orden_id, lineas (sku, cantidad) |

Los datos fiscales no aparecen en ningún evento.

## 14. Outbox

El evento se guarda en `outbox_eventos` dentro de la misma transacción que el cambio de orden. Si una línea de la recepción falla, se revierte toda la recepción y no sale ningún evento.

## 15. Observabilidad

- Salud: `GET /actuator/health`. Métricas: `GET /actuator/prometheus`.
- Logs JSON ECS. Nunca registran RFC, correo ni razón social.
- Pendiente: métricas de negocio (`ordenes_compra_total`, `recepciones_total`) y trazas.

## 16. Pruebas

- Unitarias (10): normalización y validación de proveedor, reglas de orden y recepción, y cifrado con AES-GCM e índice HMAC.
- Integración (1): PostgreSQL 17 y Kafka 4.0.0 con Testcontainers. Cubre RFC duplicado, aprobación solo por DIRECCION, recepciones parciales y totales, y enmascarado.

```bash
mvn -B verify
mvn -B package -DskipITs
```

## 17. Despliegue

Imagen multi-etapa, usuario sin privilegios (uid 10001), `HEALTHCHECK`. Despliegue con `docker compose up -d --build`.

## 18. Rollback

Las migraciones no revierten solas. Para volver atrás, despliega la imagen anterior compatible. **Nunca cambies las claves de cifrado sin migrar los datos**: los RFC cifrados quedarían ilegibles.

## 19. Solución de problemas

- **"Defina COMPRAS_CIFRADO_CLAVE":** falta la clave en `.env`.
- **El arranque falla con "debe ser una clave AES-256":** la clave no tiene 32 bytes decodificados. Genérala con `openssl rand -base64 32`.
- **409 "Ya existe un proveedor con ese RFC":** el RFC ya está dado de alta.
- **409 "La recepción excede lo ordenado":** consulta `GET /ordenes-compra/{id}` para ver lo pendiente.

## 20. Decisiones y pendientes

- **Añadido:** `GET /ordenes-compra/{id}`, para consultar el estado de cada compra.
- **Pendiente:** confirmar quién aprueba y el monto límite de aprobación. Hoy solo DIRECCION aprueba, sin límite.
- **Pendiente:** endpoints para cambiar el estatus del proveedor (inactivar o bloquear). Hoy todos nacen `ACTIVO`.
- **Pendiente:** rotación de claves de cifrado con re-cifrado de datos.
- **Pendiente:** pago a proveedores desde facturacion-pagos-ms y portal de proveedores.
- **Pendiente:** `Idempotency-Key`, métricas de negocio y diagramas en PNG.

## 21. Referencias

- Especificación del área: `area_md_specification_files/Compras_Proveedores.md`
- Diseño técnico: `docs/diseno.md`
- Orden de arranque general: `README.md` de la raíz, sección 10

## Descubrimiento, gateway y monitoreo

- Al arrancar, `compras-proveedores-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/compras-proveedores-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Idempotencia de altas (`Idempotency-Key`)

El alta de `POST /v1/ordenes-compra` acepta la cabecera opcional `Idempotency-Key`. Úsela cuando un reintento no debe crear el registro dos veces (por ejemplo, si se cortó la conexión y no sabe si la petición llegó).

- **Misma clave y mismo cuerpo:** responde la misma respuesta original; no se crea nada nuevo.
- **Misma clave con otro cuerpo:** responde `422` (`regla-de-negocio`).
- **Misma clave mientras la primera petición sigue en proceso:** responde `409` (`conflicto-de-estado`).
- **Sin la cabecera:** la petición se procesa como siempre.

Las claves se guardan 24 horas en la tabla `idempotencia`. Si la operación falla, la clave se libera para que el cliente pueda reintentar.


## Indicadores para reportes ejecutivos

Publica un indicador mensual al tópico `indicadores-areas`, que consume `reportes-ejecutivos-ms`.

- **Indicador:** `ordenes_mes` (área `COMPRAS_PROVEEDORES`).
- **Definición:** Número de órdenes de compra creadas en el mes, en cualquier estatus (incluidas canceladas). Las órdenes anteriores a V3 reciben, en la migración V4, la fecha en que se aplicó esa migración.
- **Indicador adicional:** `ordenes_canceladas_mes` (mismo área). Cuenta solo las órdenes en estatus CANCELADA del mes.
- **Clave del mensaje:** `COMPRAS_PROVEEDORES|ordenes_mes|AAAA-MM`. Reportes actualiza el valor por área, nombre y periodo (upsert).
- **Ciclo:** recalcula el mes actual y el anterior cada 60 minutos; el primer envío ocurre a los 30 segundos.
- **Configuración:** `INDICADORES_TOPICO`, `INDICADORES_INTERVALO_MS`, `INDICADORES_INICIO_MS`.
- **Entrega:** no pasa por el outbox. Es una foto del periodo; si un envío falla, el siguiente ciclo lo corrige.
- **Clase:** `PublicadorIndicadoresOrdenes`. Prueba: `mvn verify -Dit.test='PublicadorIndicadores*IT'`.
