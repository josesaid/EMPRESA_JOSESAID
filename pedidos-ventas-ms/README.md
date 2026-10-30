# pedidos-ventas-ms

## 1. Identidad

- **Nombre:** pedidos-ventas-ms
- **Área de negocio:** Pedidos y ventas
- **Puerto en tu Mac:** 8107 (dentro del contenedor escucha en 8081)
- **Base de datos:** `pedidos`, usuario `pedidos` (creados por `infra/postgres/init-databases.sh`)
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · PostgreSQL 17 · Kafka 4.0.0

## 2. Propósito y alcance

Gestiona el ciclo comercial de venta: cotizaciones que se aceptan y se convierten en pedidos, confirmación, entrega, cancelación, devolución y métricas de ventas netas.

Incluye:
- Cotizaciones con vigencia (BORRADOR → ENVIADA → ACEPTADA, o VENCIDA).
- Pedidos (BORRADOR → CONFIRMADO → ENTREGADO, con CANCELADO como salida).
- Cálculo de subtotal, IVA y total con tasas del catálogo y redondeo bancario.
- Métricas de ventas por mes y moneda.

No incluye (por ahora): clientes (vive en clientes-ms), facturación (facturacion-pagos-ms), inventario (inventario-operaciones-ms) ni cobro. Ver sección 20.

## 3. Estructura del repositorio

```
pedidos-ventas-ms/
├── Dockerfile                 Build multi-etapa (JDK 21 → JRE 21), usuario no root
├── docker-compose.yml         Servicio con comentarios; se incluye desde la raíz
├── .env.example               Variables del servicio (copie a .env sin subir secretos)
├── pom.xml                    Dependencias heredadas del padre de la empresa
├── docs/diseno.md             Diseño de dominio y decisiones
└── src/
    ├── main/java/.../pedidos/
    │   ├── domain/            Cotizacion, Pedido, Linea, Totales, reglas y excepciones
    │   ├── application/       PedidosService (casos de uso), eventos y publicador
    │   ├── infrastructure/
    │   │   ├── persistence/   Entidades JPA y adaptadores de repositorio
    │   │   └── outbox/        Outbox y retransmisor a Kafka
    │   ├── api/               PedidosControlador y ManejadorErrores (RFC 9457)
    │   └── config/            Seguridad JWT, reloj y propiedades
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__esquema_inicial.sql
    └── test/java/.../pedidos/
        ├── domain/            Pruebas unitarias de reglas
        └── api/PedidosIT.java Pruebas de integración con Testcontainers
```

## 4. Requisitos

- macOS con Docker Desktop en ejecución.
- JDK 21 (solo si corre fuera de Docker) y Maven 3.9+.
- Red `empresa-net` creada por la raíz.
- postgres y kafka levantados desde la raíz.
- Valida tokens con la clave pública de usuarios-accesos-ms (JWKS), sin secreto compartido.

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PEDIDOS_DB_PASSWORD` | Sí (compose) | Contraseña del usuario de base `pedidos`. |
| `JWT_JWKS_URI` | No | URL del JWKS de usuarios-accesos-ms (clave pública). Ya viene configurada en el Compose |
| `JWT_DURACION` | No | Duración del token aceptado, ISO-8601. Por omisión `PT15M`. |
| `OUTBOX_INTERVALO_MS` | No | Cada cuántos ms se envían eventos pendientes. Por omisión 5000. |
| `KAFKA_BOOTSTRAP` | No | Dentro de compose es `kafka:29092`. |

El servicio no arranca si el clave pública JWKS falta o es menor a 32 bytes.

## 6. Ejecución

Desde la raíz del repositorio (recomendado):

```bash
cp .env.example .env            # una sola vez; complete las variables de la raíz
docker compose up -d postgres kafka
cd pedidos-ventas-ms
docker compose up -d --build
curl -s http://localhost:8107/actuator/health
```

Sin Docker, con Maven:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pedidos DB_USER=pedidos DB_PASSWORD=...
export JWT_JWKS_URI=http://localhost:8102/.well-known/jwks.json
mvn spring-boot:run
```

## 7. Modelo de datos

| Tabla | Contenido |
|---|---|
| `cotizaciones` | Cabecera, estatus, vigencia, `pedido_id` único (se convierte una sola vez). |
| `lineas_cotizacion` | Líneas por posición; precio congelado al crear. |
| `pedidos` | Cabecera, estatus, fechas de confirmación y entrega, motivo de cancelación. |
| `lineas_pedido` | Líneas del pedido; se congelan al confirmar. |
| `outbox_eventos` | Eventos pendientes de enviar a Kafka. |

Montos en `NUMERIC(14,2)`. `version` en cada agregado para control optimista.

## 8. Migraciones

Flyway ejecuta `V1__esquema_inicial.sql` al arrancar. Hibernate solo valida (`ddl-auto: validate`). Los cambios futuros serán nuevas migraciones `V2`, `V3`, etc.; nunca se editan migraciones ya aplicadas.

## 9. Máquinas de estado

**Cotización:** BORRADOR →(enviar)→ ENVIADA →(aceptar, dentro de vigencia)→ ACEPTADA. Si la vigencia pasó, aceptar la marca VENCIDA y responde 410. VENCIDA no se reactiva: se crea una nueva cotización.

**Pedido:** BORRADOR →(confirmar, requiere ≥1 línea)→ CONFIRMADO →(entregar)→ ENTREGADO.
- CANCELADO desde BORRADOR o CONFIRMADO, con motivo `CANCELACION_CLIENTE` o `ERROR_CAPTURA`.
- Devolución total desde ENTREGADO: queda CANCELADO con motivo `DEVOLUCION_TOTAL`.
- Las líneas solo cambian en BORRADOR.
- Nada se borra físicamente.

## 10. Seguridad

- API sin estado con token JWT RS256 (resource server). Solo `/actuator/health` es público.
- Roles en el claim `roles` del token, convertidos a `ROLE_*`.
- Autorización por método con `@PreAuthorize`.

| Operación | Roles |
|---|---|
| Crear y operar cotización y pedido | VENTAS, DIRECCION |
| Entregar pedido | ALMACEN, DIRECCION |
| Consultar pedidos y cotizaciones | VENTAS, DIRECCION, FINANZAS (ALMACEN solo pedido) |
| Métricas de ventas | DIRECCION, FINANZAS |
| Catálogos | Cualquier usuario autenticado |

## 11. API

Base `/v1`. Errores en `application/problem+json` (RFC 9457).

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | /cotizaciones | Crea cotización | 201 |
| GET | /cotizaciones/{id} | Consulta cotización | 200 |
| POST | /cotizaciones/{id}/envio | Envía a cliente | 200 |
| POST | /cotizaciones/{id}/aceptacion | Acepta (410 si venció) | 200 |
| POST | /cotizaciones/{id}/pedido | Convierte en pedido (una vez) | 201 |
| POST | /pedidos | Crea pedido directo | 201 |
| GET | /pedidos?clienteId=&estatus=&canal= | Lista con filtros | 200 |
| GET | /pedidos/{id} | Consulta pedido | 200 |
| PUT | /pedidos/{id}/lineas | Reemplaza líneas (solo BORRADOR) | 200 |
| POST | /pedidos/{id}/confirmacion | Confirma | 200 |
| POST | /pedidos/{id}/entrega | Registra entrega | 200 |
| POST | /pedidos/{id}/cancelacion | Cancela con `motivo` | 200 |
| POST | /pedidos/{id}/devolucion | Devolución total | 200 |
| GET | /metricas/ventas?mes=2026-10 | Ventas netas del mes | 200 |
| GET | /catalogos/tasas-iva | Tasas disponibles | 200 |
| GET | /catalogos/monedas | Monedas y decimales | 200 |

Códigos de error: 400 solicitud inválida, 404 no encontrado, 409 conflicto de estado o concurrencia, 410 cotización vencida, 422 regla de negocio, 403 sin rol.

Ejemplo de cotización:

```json
{"clienteId":"11111111-1111-1111-1111-111111111111","canal":"WEB","moneda":"MXN",
 "tasaIva":"IVA_GENERAL","vigenteHasta":"2026-12-31",
 "lineas":[{"productoId":"22222222-2222-2222-2222-222222222222","descripcion":"Tornillo","cantidad":2,"precioUnitario":10.00}]}
```

## 12. Reglas de duplicados

- Una cotización solo genera un pedido (`pedido_id` único y validación de estado).
- Dos peticiones simultáneas de conversión: una gana y la otra recibe 409 por control optimista.
- Idempotency-Key en POST: pendiente (sección 20).

## 13. Eventos

Se publican en el outbox dentro de la misma transacción:

| Evento | Cuándo | Datos |
|---|---|---|
| PedidoConfirmado | Pedido confirmado | clienteId, moneda, total |
| PedidoEntregado | Pedido entregado | clienteId |
| PedidoCancelado | Cancelación o devolución | motivo |

Formato del sobre: `{"tipo":"...","agregadoId":"...","datos":{...}}`. Topic de Kafka: nombre del servicio.

## 14. Outbox

`PublicadorEventosOutbox` escribe en `outbox_eventos`. `RetransmisorOutbox` lo lee cada `OUTBOX_INTERVALO_MS`, envía a Kafka y marca publicado. Si Kafka falla, el evento queda pendiente y se reintenta; se detiene al primer fallo para conservar el orden.

## 15. Observabilidad

- Salud: `GET /actuator/health`.
- Métricas Prometheus: `GET /actuator/prometheus` (se conecta cuando exista monitoreo de la empresa).
- Logs en formato JSON ECS.
- Trazabilidad de requests: pendiente de contenedor de tracing de la empresa.

## 16. Pruebas

```bash
mvn -B test     # unitarias de reglas de dominio (8)
mvn -B verify   # además PedidosIT con PostgreSQL y Kafka reales (requiere Docker)
```

Script de verificación en Mac, desde la raíz: `bash ejecutar-pruebas-pedidos.sh`. Resume cada paso y guarda registros `.verify-pedidos-ventas-ms.log` y `.build-pedidos-ventas-ms.log`.

## 17. Despliegue

Se despliega como contenedor con la imagen `pedidos-ventas-ms:0.1.0`. Requiere la red `empresa-net`, la base `pedidos` y el clave pública JWKS. Un único replica es suficiente para el alcance actual.

## 18. Rollback

1. `docker compose stop pedidos-ventas-ms`.
2. Regrese a la imagen anterior con su etiqueta.
3. Las migraciones Flyway no se revierten automáticamente. Si una migración fue incorrecta, se corrige con una migración nueva.

## 19. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| Arranque falla con "secreto debe tener al menos 32 bytes" | `JWT_JWKS_URI` no responde | Revise `.env` de la raíz. |
| Error 401 con token válido | Secreto distinto al de usuarios-accesos-ms | Copie el mismo valor. |
| Error de conexión a la base | Base `pedidos` no creada en un volumen existente | Ejecute el `CREATE ROLE` y `CREATE DATABASE` del README de la raíz. |
| 409 al convertir cotización | Ya convertida | Consulte `pedido_id` de la cotización. |
| 410 al aceptar | Venció la vigencia | Cree una nueva cotización. |

## 20. Decisiones y pendientes

- **IVA_FRONTERIZO (8%)**: tasa tomada del catálogo propuesto; falta validación con Contabilidad.
- **Rol Gerente de ventas**: no existe en el catálogo de usuarios-accesos; se usa DIRECCION. Pendiente de decisión.
- **Idempotency-Key**: pendiente en POST.
- **Validación de cliente existente**: pendiente de llamada a clientes-ms (hoy el clienteId se acepta como referencia).
- **Métricas de ventas**: ventas netas; excluye cancelados y devueltos.
- **Inventario al confirmar**: no reserva stock todavía; se integrará con inventario-operaciones-ms.
- **Refresh/logout**: heredado de usuarios-accesos; pendiente de definir si aplica aquí.

## 21. Referencias

- Diseño detallado: `docs/diseno.md`.
- Arranque y dependencias: sección "Orden de arranque" del README de la raíz.
- Especificación del área: `area_md_specification_files/Pedidos_Ventas.md`.
- Servicio de tokens: `usuarios-accesos-ms`.

## Descubrimiento, gateway y monitoreo

- Al arrancar, `pedidos-ventas-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/pedidos-ventas-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Idempotencia de altas (`Idempotency-Key`)

El alta de `POST /v1/pedidos` acepta la cabecera opcional `Idempotency-Key`. Úsela cuando un reintento no debe crear el registro dos veces (por ejemplo, si se cortó la conexión y no sabe si la petición llegó).

- **Misma clave y mismo cuerpo:** responde la misma respuesta original; no se crea nada nuevo.
- **Misma clave con otro cuerpo:** responde `422` (`regla-de-negocio`).
- **Misma clave mientras la primera petición sigue en proceso:** responde `409` (`conflicto-de-estado`).
- **Sin la cabecera:** la petición se procesa como siempre.

Las claves se guardan 24 horas en la tabla `idempotencia`. Si la operación falla, la clave se libera para que el cliente pueda reintentar.


## Indicadores para reportes ejecutivos

Publica un indicador mensual al tópico `indicadores-areas`, que consume `reportes-ejecutivos-ms`.

- **Indicador:** `ventas_mes` (área `VENTAS_ATENCION_CLIENTES`).
- **Definición:** Suma de líneas de pedidos en MXN con estatus CONFIRMADO o ENTREGADO, con IVA incluido, asignada al mes de su confirmación (hora de la Ciudad de México).
- **Clave del mensaje:** `VENTAS_ATENCION_CLIENTES|ventas_mes|AAAA-MM`. Reportes actualiza el valor por área, nombre y periodo (upsert).
- **Ciclo:** recalcula el mes actual y el anterior cada 60 minutos; el primer envío ocurre a los 30 segundos.
- **Configuración:** `INDICADORES_TOPICO`, `INDICADORES_INTERVALO_MS`, `INDICADORES_INICIO_MS`.
- **Entrega:** no pasa por el outbox. Es una foto del periodo; si un envío falla, el siguiente ciclo lo corrige.
- **Clase:** `PublicadorIndicadoresVentas`. Prueba: `mvn verify -Dit.test='PublicadorIndicadores*IT'`.
