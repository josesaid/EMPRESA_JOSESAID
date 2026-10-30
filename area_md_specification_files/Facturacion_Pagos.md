# Facturación y pagos

**Área:** Finanzas y contabilidad
**Servicio propuesto:** facturacion-pagos-ms

## Rol
Eres el microservicio de facturación y pagos. Emites comprobantes fiscales a partir de órdenes confirmadas, registras cobros y consultas saldos.

## Contexto
**Incluye:** emisión de comprobantes fiscales (CFDI 4.0, propuesto), registro de pagos recibidos, cálculo de saldo pendiente y cancelación con motivo.
**Excluye:** cotizaciones y órdenes (Pedidos y ventas), asientos contables (Contabilidad), timbrado con PAC (pendiente de integración).

## Tarea
Crear un comprobante desde una orden confirmada, registrar pagos parciales o totales, consultar saldos y cancelar comprobantes.

## Restricciones
- Un comprobante emitido no se modifica; se cancela y se emite otro.
- Montos con `BigDecimal`.
- No se almacenan datos de tarjeta; solo la referencia del procesador.

## Formato de salida
JSON. Montos como cadena decimal con código de moneda ISO 4217.

## Escenarios de entrada
1. Llega el evento de orden confirmada → se crea comprobante en estatus BORRADOR.
2. Se emite el comprobante → estatus EMITIDO.
3. Llega un pago → se aplica al saldo.
4. Se solicita cancelación con motivo → estatus CANCELADO.

## Reglas de negocio (propuestas)
- Un pago no puede exceder el saldo pendiente.
- Un comprobante cancelado no se reactiva.
- Cancelar requiere motivo.
- Los impuestos se calculan con reglas de esta área (propuesta: IVA 16% en México, por validar).

## Catálogos
- Formas de pago y uso de CFDI (catálogos SAT, propuesta).
- Monedas ISO 4217.

## Entidades
- **Comprobante:** id (UUID), orden_id (UUID), folio, subtotal, impuestos, total, moneda, estatus (BORRADOR, EMITIDO, CANCELADO), version, creado_utc (Instant).
- **Pago:** id (UUID), comprobante_id (UUID), monto (BigDecimal), fecha_pago (LocalDate), referencia, creado_utc (Instant).
- **Estatus de cobro (derivado):** PENDIENTE, PARCIAL, PAGADO.

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /comprobantes | FINANZAS | 201 | 400, 409 |
| GET | /comprobantes/{id} | FINANZAS, VENTAS | 200 | 404 |
| POST | /comprobantes/{id}/emision | FINANZAS | 200 | 404, 409 |
| POST | /comprobantes/{id}/cancelacion | FINANZAS | 200 | 400, 404, 409 |
| POST | /comprobantes/{id}/pagos | FINANZAS | 201 | 400, 404, 422 |
| GET | /comprobantes/{id}/pagos | FINANZAS | 200 | 404 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| comprobante-emitido | Emisión exitosa | id, total, moneda |
| pago-registrado | Pago aplicado | id, comprobante_id, monto |
| comprobante-cancelado | Cancelación | id, motivo |

## Requisitos funcionales
- Emitir comprobante desde una orden confirmada.
- Registrar pagos parciales y totales.
- Cancelar con motivo.

## Requisitos no funcionales
- Disponibilidad objetivo 99.5% (propuesta).
- Latencia p95 de lectura menor a 300 ms (propuesta).

## Seguridad y acceso
- FINANZAS: lectura y escritura.
- VENTAS: solo lectura.
- Autenticación OAuth2/JWT emitida por Usuarios y accesos.
- RFC enmascarado en respuestas.

## Observabilidad
- Métricas: comprobantes_emitidos_total, pagos_registrados_total, errores 5xx.
- Logs sin RFC ni montos por cliente.

## Criterios de aceptación
- Un pago mayor al saldo devuelve 422.
- Una cancelación sin motivo devuelve 400.

## Pruebas
- Unitarias: reglas de saldo y estatus.
- Integración: PostgreSQL y Kafka con Testcontainers, incluido outbox.
- Contrato: endpoints y eventos.

## Despliegue y rollback
Canary con criterio del anexo de la empresa. Rollback por contenedor. Las correcciones se hacen como compensaciones.

## Pendientes por integración futura
- Timbrado con PAC.
- Consumo del evento de orden confirmada desde Pedidos y ventas.
- Conciliación bancaria.

## Comentarios
- El stack no está confirmado. Por defecto se usa el de crea-area-empresa.

## Roles en el área
- **FINANZAS:** emite, cobra y cancela.
- **VENTAS:** consulta.
