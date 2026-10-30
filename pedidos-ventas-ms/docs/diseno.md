# Diseño de pedidos-ventas-ms

## Contexto

Ventas necesita convertir cotizaciones aceptadas en pedidos, controlar su estado y medir ventas netas. El servicio es dueño de cotizaciones, pedidos y su historial. No es dueño de clientes, inventario ni facturación.

## Lenguaje ubicuo

- **Cotización:** propuesta comercial con vigencia.
- **Pedido:** compromiso de venta confirmado o en preparación.
- **Línea:** producto, cantidad y precio unitario congelado.
- **Devolución:** cancelación de un pedido ya entregado.
- **Ventas netas:** pedidos confirmados o entregados, sin cancelados ni devueltos.

## Agregados

**Cotizacion** (raíz). Invariantes: al menos una línea; cantidad mayor a cero; precio no negativo; moneda del catálogo; solo ENVIADA puede aceptarse; una sola conversión a pedido.

**Pedido** (raíz). Invariantes: solo BORRADOR cambia líneas; confirmar exige una línea; entregar exige CONFIRMADO; cancelar exige motivo y estado BORRADOR o CONFIRMADO; devolver exige ENTREGADO; DEVOLUCION_TOTAL no se usa en cancelación manual.

## Decisiones

1. **Precio congelado.** La línea guarda precio y tasa al crear o confirmar. Un cambio posterior en el catálogo no altera pedidos existentes.
2. **Redondeo bancario.** Subtotal, IVA y total se redondean a los decimales de la moneda con `HALF_EVEN`.
3. **Vencimiento persistido.** Aceptar una cotización vencida la deja VENCIDA y responde 410. Por eso la transacción no se revierte en ese caso.
4. **Devolución como transición.** No se borra el pedido; pasa a CANCELADO con motivo DEVOLUCION_TOTAL. Así queda rastro para auditoría.
5. **Ventas netas.** Las métricas filtran por estatus actual (CONFIRMADO o ENTREGADO) y por fecha de confirmación.
6. **Control optimista.** Cada agregado tiene `version`. Una escritura concurrente responde 409.

## Eventos

Se emiten PedidoConfirmado, PedidoEntregado y PedidoCancelado por el outbox. Los consumidores (facturación, contabilidad, inventario) reaccionan de forma asíncrona.

## Seguridad

JWT RS256 validado con la clave pública de usuarios-accesos-ms (JWKS). Autorización por rol con `@PreAuthorize`. Los errores no exponen detalles internos.

## Riesgos y pendientes

- IVA_FRONTERIZO sin validación de Contabilidad.
- Falta el rol Gerente de ventas en el catálogo.
- Sin validación de cliente contra clientes-ms.
- Sin reserva de inventario al confirmar.
- Sin Idempotency-Key en POST.

## Fuera de alcance

Cobro, emisión de CFDI, descuentos por volumen y promociones.


## Indicadores para reportes

- **Indicador:** `ventas_mes` (área `VENTAS_ATENCION_CLIENTES`). Ventas del mes: líneas de pedidos MXN CONFIRMADO/ENTREGADO, con IVA, por mes de confirmación.
- **Publicación:** clase `PublicadorIndicadoresVentas`, cada 60 minutos, a `indicadores-areas`. Clave `VENTAS_ATENCION_CLIENTES|ventas_mes|AAAA-MM`.
- **Decisión:** es una foto del periodo, no un evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Consumo:** `reportes-ejecutivos-ms` lo guarda con upsert por área, nombre y periodo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).


## Validación de cliente

Antes de crear una cotización o un pedido, el servicio consulta `GET /v1/clientes/{id}` en clientes-ms, con el mismo token del usuario.

| Caso | Respuesta |
|---|---|
| Cliente inexistente | 422 `regla-de-negocio`: "El cliente no existe" |
| Cliente SUSPENDIDO o BAJA | 422 `regla-de-negocio`: "El cliente no está activo" |
| clientes-ms caído o con error | 503 `servicio-no-disponible`: reintentar |
| Cliente ACTIVO | La operación continúa |

- **Configuración:** `CLIENTES_URL`, por omisión `http://clientes-ms:8080`.
- **Decisión:** consulta síncrona, aprobada. Si clientes-ms no responde, pedidos tampoco crea nada: prefiere rechazar a guardar un cliente no verificado.
- **Pruebas:** `ClientesRestValidadorTest` con servidor REST simulado; `PedidosIT` sustituye el validador con `@MockitoBean`.
