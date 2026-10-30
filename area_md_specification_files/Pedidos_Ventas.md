# Pedidos y ventas

**Área:** Ventas y atención a clientes
**Servicio propuesto:** pedidos-ventas-ms

## Rol
Eres el microservicio de pedidos y ventas. Registras cotizaciones, órdenes y su estado, desde la venta hasta la entrega.

## Contexto
**Incluye:** cotizaciones, conversión de cotización a pedido, edición de líneas, confirmación, entrega, cancelación, devolución total, catálogo de monedas, tasas de IVA, canales de venta y métricas de ventas.
**Excluye:** clientes (Clientes), inventario y pagos (pendientes de microservicios futuros) y facturación (Facturación y pagos).
**Consumidores:** interfaz de ventas y Facturación y pagos.

## Tarea
Crear cotizaciones, convertirlas en pedidos, confirmar y entregar pedidos, registrar cancelaciones y devoluciones, y consultar el estado de cada venta.

## Restricciones
- Los montos usan `BigDecimal` con redondeo bancario a los decimales de la moneda.
- No se almacenan datos de tarjeta.
- Las fechas se guardan en UTC; se convierten a `America/Mexico_City` solo en presentación.
- No registrar en logs montos asociados a un cliente identificable ni datos de contacto.

## Formato de salida
JSON. Diagramas en SVG o Mermaid, conforme a crea-area-empresa.

## Escenarios de entrada
1. **Cotización:** un vendedor cotiza dos productos en MXN. El sistema calcula IVA general de 16% y muestra el total.
2. **Cotización aceptada:** el cliente acepta la cotización y se convierte en pedido.
3. **Pedido directo en web:** un cliente compra desde la web; se calcula el total antes de confirmar.
4. **Pedido en tienda:** en mostrador se aplica la tasa fronteriza de 8% según la dirección de entrega.
5. **Pedido por teléfono:** el canal queda registrado como `TELEFONO`.
6. **Devolución:** un pedido entregado se devuelve completo; se registra como cancelación con motivo `DEVOLUCION_TOTAL`.
7. **Métricas:** un gerente consulta pedidos confirmados por canal y el monto total del mes.

## Reglas de negocio
- **Cotización:** `BORRADOR → ENVIADA → ACEPTADA` (se convierte en pedido), o `RECHAZADA` o `VENCIDA`. Una cotización vencida no se reactiva; se crea una nueva.
- Solo un pedido en `BORRADOR` admite cambios en sus líneas.
- **Pedido:** `BORRADOR → CONFIRMADO → ENTREGADO`. `CANCELADO` se alcanza desde `BORRADOR` o `CONFIRMADO`. Una devolución pasa de `ENTREGADO` a `CANCELADO`.
- Un pedido `CANCELADO` no se modifica ni se reactiva.
- Al confirmar se congelan precio, moneda y tasa de IVA de cada línea.
- La devolución es siempre total y solo desde `ENTREGADO`.
- Confirmar requiere al menos una línea con cantidad mayor a cero.
- Toda cancelación exige motivo: `CANCELACION_CLIENTE`, `ERROR_CAPTURA` o `DEVOLUCION_TOTAL`. Nada se borra.
- El canal es obligatorio: `WEB`, `TIENDA` o `TELEFONO`.

## Catálogos
- **Monedas:** código ISO 4217, nombre, decimales y bandera de activa. Inicial: MXN y USD (2 decimales). Agregar una moneda es configuración.
- **Tasas de IVA:** `IVA_GENERAL` 16%; `IVA_FRONTERIZO` 8% (zona fronteriza norte, por validar con contabilidad); `IVA_EXENTO` 0%. Cada tasa tiene `vigenteDesde` y `vigenteHasta`.
- **Canales:** `WEB`, `TIENDA`, `TELEFONO`.

## Entidades
- **Cotizacion:** `UUID id`, `UUID clienteId`, `EstatusCotizacion estatus`, `String monedaCodigo`, `BigDecimal total`, `LocalDate vigenteHasta`, `long version`.
- **Pedido:** `UUID id`, `UUID clienteId`, `UUID cotizacionId` (opcional), `CanalVenta canal`, `EstatusPedido estatus`, `UUID vendedorId`, `String monedaCodigo`, `BigDecimal subtotal`, `BigDecimal iva`, `BigDecimal total`, `String tasaIvaCodigo`, `LocalDate fechaPedido`, `Instant fechaConfirmacion`, `Instant fechaEntrega`, `MotivoCancelacion motivoCancelacion`, `long version`.
- **LineaPedido:** `UUID id`, `UUID pedidoId`, `UUID productoId`, `String descripcionSnapshot`, `int cantidad`, `BigDecimal precioUnitario`, `BigDecimal subtotal`.

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/v1/cotizaciones` | Vendedor | 201 | 400, 404 (cliente), 422 |
| POST | `/v1/cotizaciones/{id}/enviar` | Vendedor | 200 | 409 |
| POST | `/v1/cotizaciones/{id}/aceptar` | Vendedor | 200 | 409, 410 (vencida) |
| POST | `/v1/cotizaciones/{id}/pedido` | Vendedor | 201 | 409, 422 |
| POST | `/v1/pedidos` | Vendedor | 201 | 400, 404, 422 |
| PUT | `/v1/pedidos/{id}/lineas` | Vendedor | 200 | 409, 422 |
| POST | `/v1/pedidos/{id}/confirmar` | Vendedor | 200 | 409, 422 |
| POST | `/v1/pedidos/{id}/entregar` | Vendedor | 200 | 409 |
| POST | `/v1/pedidos/{id}/cancelar` | Gerente de ventas | 200 | 400, 409 |
| POST | `/v1/pedidos/{id}/devolver` | Gerente de ventas | 200 | 409 |
| GET | `/v1/pedidos?clienteId=&estatus=&canal=` | Vendedor, Gerente | 200 | 400 |
| GET | `/v1/catalogos/monedas` | Todos | 200 | — |
| GET | `/v1/catalogos/tasas-iva` | Todos | 200 | — |
| GET | `/v1/metricas/ventas?mes=2026-10` | Gerente de ventas | 200 | 400 |

Las escrituras aceptan `Idempotency-Key`. Concurrencia optimista con `version`.

**Ejemplo de respuesta (POST `/v1/pedidos/{id}/confirmar`):**
```json
{
  "id": "7f3c2a1e-5b2d-4c1a-9e8f-2a4b6c8d0e1f",
  "canal": "WEB",
  "estatus": "CONFIRMADO",
  "moneda": "MXN",
  "subtotal": 1000.00,
  "tasaIva": "IVA_GENERAL",
  "iva": 160.00,
  "total": 1160.00
}
```

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| `PedidoConfirmado` | Confirmar | id, clienteId, canal, moneda, tasaIva, totales, líneas |
| `PedidoEntregado` | Entregar | id, fechaEntrega |
| `PedidoCancelado` | Cancelar o devolver | id, motivo |

`PedidoConfirmado` dispara la facturación. Se publican con el patrón outbox.

## Requisitos funcionales
- Crear cotizaciones y convertirlas en pedidos.
- Crear pedidos con validación de cliente, moneda y canal.
- Editar líneas solo en borrador.
- Confirmar, entregar, cancelar y devolver.
- Consulta paginada de pedidos por cliente, estatus y canal.
- Métricas de ventas por mes, canal y monto.

## Requisitos no funcionales
- Identificador único y auditoría (creación y modificación en UTC).
- Operaciones de estatus idempotentes.
- Consulta por identificador en menos de 300 ms (p95).
- Disponibilidad objetivo de 99.9%.

## Seguridad y acceso
- OAuth2/JWT emitido por Usuarios y accesos.
- Autorización por rol (ver endpoints).
- TLS en tránsito.

## Observabilidad
- Métricas: cotizaciones, pedidos creados, confirmados, cancelados, devueltos, monto total y latencia por endpoint (Micrometer + Prometheus).
- Trazas con OpenTelemetry, con correlación hacia Clientes y Facturación.
- Logs JSON estructurados.
- Nunca se registra: montos asociados a un cliente identificable, datos de contacto ni contenido de líneas.

## Criterios de aceptación
- Un pedido con al menos una línea válida pasa a `CONFIRMADO` en una sola operación y emite `PedidoConfirmado`.
- El IVA usa la tasa vigente al confirmar y se redondea a los decimales de la moneda.
- Solo un pedido `ENTREGADO` puede devolverse, como cancelación total.
- Un pedido confirmado rechaza cambios en sus líneas con 409.
- Una cotización aceptada se convierte en pedido una sola vez.

## Pruebas
- Unitarias: transiciones de estado de cotización y pedido, IVA y redondeo.
- Integración: PostgreSQL y Kafka con Testcontainers, incluido el outbox.
- Contrato: con Clientes y con Facturación.

## Despliegue y rollback
- Canary con 10% del tráfico de ventas. Criterio del anexo: 5xx menor a 1% durante 30 minutos.
- Migraciones con Flyway, compatibles hacia atrás.
- Rollback por contenedor. Cancelaciones y devoluciones son compensaciones, nunca borrados.

## Pendientes por integración futura
- **Inventario:** reserva de stock al confirmar y liberación al cancelar o devolver.
- **Pagos:** estado de pago, anticipos y reembolsos.

## Comentarios
- Definir si el `Idempotency-Key` se conserva 24 horas.
- Validar con contabilidad la tasa fronteriza antes de producción.

## Roles en el área
- **Vendedor:** crea cotizaciones, pedidos, confirma y entrega; consulta.
- **Gerente de ventas:** cancela, devuelve y consulta métricas.
