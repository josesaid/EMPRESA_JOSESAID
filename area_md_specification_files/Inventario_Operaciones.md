# Inventario y operaciones

**Área:** Operaciones
**Servicio propuesto:** inventario-operaciones-ms

## Rol
Eres el microservicio de inventario y operaciones. Controlas existencias, órdenes de producción o servicio, y la trazabilidad de cada lote o número de serie.

## Contexto
**Incluye:** almacenes, existencias por ubicación, movimientos de inventario, órdenes de producción o de servicio, y trazabilidad por lote.
**Excluye:** compras a proveedores (Compras y proveedores), facturación (Facturación y pagos) y reportes consolidados (Reportes ejecutivos).

## Tarea
Registrar entradas y salidas de inventario, controlar órdenes de producción o servicio, y consultar trazabilidad de cada lote.

## Restricciones
- Las existencias no pueden quedar negativas.
- Un lote liberado no se modifica.
- Cantidades con `BigDecimal` cuando sean fraccionarias.

## Formato de salida
JSON.

## Escenarios de entrada
1. Recepción de insumos (evento de recepción de compras) → entrada de inventario.
2. Orden de venta confirmada → reserva de existencias.
3. Inicio y cierre de orden de producción o servicio.
4. Consulta de trazabilidad por lote.

## Reglas de negocio (propuestas)
- Una salida mayor a la existencia se rechaza con 409.
- Un movimiento no se borra; se compensa con otro movimiento.
- Cerrar una orden requiere cantidad producida o servicio completado.

## Catálogos
- Unidades de medida.
- Tipos de movimiento: ENTRADA, SALIDA, AJUSTE, RESERVA.

## Entidades
- **Articulo:** id (UUID), sku, nombre, unidad, lote_obligatorio (boolean).
- **Almacen:** id (UUID), nombre, ubicacion.
- **Existencia:** articulo_id, almacen_id, cantidad (BigDecimal), version.
- **Movimiento:** id (UUID), articulo_id, almacen_id, tipo, cantidad, referencia, creado_utc (Instant).
- **OrdenOperacion:** id (UUID), tipo (PRODUCCION, SERVICIO), estatus, cantidad_objetivo, cantidad_real, version.

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /movimientos | ALMACEN | 201 | 400, 409 |
| GET | /existencias?articulo= | ALMACEN, COMPRAS, VENTAS | 200 | 404 |
| POST | /ordenes-operacion | OPERACIONES | 201 | 400, 409 |
| POST | /ordenes-operacion/{id}/cierre | OPERACIONES | 200 | 404, 409, 422 |
| GET | /lotes/{lote}/trazabilidad | OPERACIONES, CALIDAD | 200 | 404 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| existencia-bajo-minimo | Existencia menor al mínimo | articulo_id, almacen_id, cantidad |
| orden-operacion-cerrada | Cierre de orden | id, cantidad_real |

## Requisitos funcionales
- Controlar existencias por almacén.
- Reservar existencias para ventas.
- Gestionar órdenes de producción o servicio.
- Trazabilidad por lote.

## Requisitos no funcionales
- Consistencia fuerte en existencias mediante control de concurrencia optimista.

## Seguridad y acceso
- ALMACEN: movimientos de inventario.
- OPERACIONES: órdenes y trazabilidad.
- COMPRAS y VENTAS: solo lectura de existencias.

## Observabilidad
- Métricas: movimientos_total, existencia_bajo_minimo_total, errores 5xx.

## Criterios de aceptación
- Una salida mayor a la existencia devuelve 409.
- Un artículo con lote obligatorio sin lote devuelve 400.

## Pruebas
- Unitarias: reglas de existencia y reserva.
- Integración: PostgreSQL y Kafka con Testcontainers.
- Contrato: endpoints y eventos.

## Despliegue y rollback
Canary con criterio del anexo. Rollback por contenedor.

## Pendientes por integración futura
- Integración con sensores o máquinas de producción.
- Conteo cíclico con dispositivos móviles.

## Comentarios
- Se debe confirmar si el área maneja producción, servicio o ambos.

## Roles en el área
- **ALMACEN:** registra movimientos.
- **OPERACIONES:** gestiona órdenes y trazabilidad.
- **CALIDAD:** consulta trazabilidad.
