# Diseño técnico: inventario-operaciones-ms

## 1. Contexto

Operaciones necesita saber en tiempo real cuánto hay de cada artículo en cada almacén, cuánto está comprometido para ventas y de qué lote viene cada unidad. La especificación está en `area_md_specification_files/Inventario_Operaciones.md`.

## 2. Objetivos

- La existencia nunca es negativa ni queda por debajo de lo reservado.
- Cada unidad es trazable por lote.
- Concurrencia segura: dos salidas simultáneas no pueden vender la misma unidad.
- Alertas de bajo mínimo sin consultar a mano.

## 3. Arquitectura (DDD)

| Capa | Contenido | Depende de |
|---|---|---|
| Dominio | `Existencia` (raíz de inventario), `MovimientoInventario`, `Articulo`, `Almacen`, `OrdenOperacion` y puertos | Nada del framework |
| Aplicación | `InventarioService`, `EventoDominio`, `PublicadorEventos` | Dominio |
| Infraestructura | Entidades JPA, adaptadores, outbox y Kafka | Aplicación y dominio |
| API | Controlador REST, DTOs y `ProblemDetail` | Aplicación |

## 4. Modelo

- **Existencia** es el agregado de control: cada `aplicar(tipo, cantidad)` valida la regla antes de cambiar nada.
- **Disponible** es `cantidad - reservado`, derivado, no almacenado.
- **MovimientoInventario** es el registro de auditoría. Se escribe en la misma transacción que el cambio de existencia.
- **OrdenOperacion** es independiente de las existencias en este alcance: su cierre registra la cantidad real, pero no consume inventario todavía (pendiente).

## 5. Concurrencia

- La fila de `existencias` tiene `@Version`. Si dos transacciones modifican la misma existencia, la segunda que confirma recibe 409 y debe reintentar.
- No hay bloqueo pesimista: las lecturas no se bloquean y el conflicto es raro en la práctica, porque cada par artículo-almacén es una fila.
- La restricción `CHECK (reservado <= cantidad)` en la base es una segunda línea de defensa si alguien escribe fuera del dominio.

## 6. Lotes y trazabilidad

- El lote es un atributo de cada movimiento, no una entidad. La trazabilidad es la lista de movimientos con ese lote, en orden de creación.
- Un artículo con `lote_obligatorio` rechaza movimientos sin lote con 400, antes de tocar la existencia.

## 7. Eventos y outbox

Igual que en los demás servicios: el evento se escribe en la misma transacción y el retransmisor lo envía en orden. El evento de bajo mínimo se publica solo cuando una salida o ajuste cruza el umbral, no en cada movimiento.

## 8. Seguridad

JWT emitido por usuarios-accesos-ms. Cada rol escribe solo lo suyo: ALMACEN mueve existencias, OPERACIONES gestiona órdenes y artículos, CALIDAD y OPERACIONES consultan trazabilidad.

## 9. Errores

RFC 9457 con `type` estable bajo `urn:josesaid:inventario-operaciones:error:`. El 400 de lote faltante se distingue del 422 de cantidad inválida, para que el cliente sepa si corregir la forma o los datos.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| Reservas que nunca se liberan y bloquean stock | Pendiente de liberación. Mientras tanto, la operación debe consumir o compensar la reserva. |
| Conflictos de concurrencia en artículos muy movidos | 409 explícito y reintento del cliente. Medir antes de añadir bloqueos. |
| Eventos duplicados a los consumidores | Consumidores idempotentes por `agregadoId` y tipo. |

## 11. Decisiones pendientes

1. Confirmar si el área maneja producción, servicio o ambos.
2. Entidad de lote con liberación.
3. Liberación de reservas y consumo automático desde ventas y compras.
4. Conteo cíclico e integración con sensores.


## Indicadores para reportes

- **Indicador:** `movimientos_mes` (área `OPERACIONES`). Movimientos del mes: conteo de movimientos de inventario por mes de creación.
- **Publicación:** clase `PublicadorIndicadoresMovimientos`, cada 60 minutos, a `indicadores-areas`. Clave `OPERACIONES|movimientos_mes|AAAA-MM`.
- **Decisión:** es una foto del periodo, no un evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Consumo:** `reportes-ejecutivos-ms` lo guarda con upsert por área, nombre y periodo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).
