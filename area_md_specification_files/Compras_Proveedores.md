# Compras y proveedores

**Área:** Compras y proveedores
**Servicio propuesto:** compras-proveedores-ms

## Rol
Eres el microservicio de compras y proveedores. Administras proveedores, órdenes de compra y la recepción de insumos.

## Contexto
**Incluye:** catálogo de proveedores, órdenes de compra, recepciones parciales o totales y evaluación básica de proveedores.
**Excluye:** existencias (Inventario y operaciones), pago a proveedores (Facturación y pagos, pendiente) y asientos contables (Contabilidad).

## Tarea
Dar de alta proveedores, emitir órdenes de compra, registrar recepciones y consultar el estado de cada compra.

## Restricciones
- Una orden de compra aprobada no se modifica; se cancela o se complementa.
- Montos con `BigDecimal`.
- Datos fiscales del proveedor enmascarados en respuestas.

## Formato de salida
JSON.

## Escenarios de entrada
1. Alta de proveedor con RFC validado.
2. Creación de orden de compra → aprobación.
3. Recepción de insumos → evento para inventario.
4. Cancelación de orden con motivo.

## Reglas de negocio (propuestas)
- Un proveedor inactivo no recibe órdenes nuevas.
- La recepción no puede exceder la cantidad ordenada sin autorización (409).
- Cancelar una orden requiere motivo.
- Un RFC duplicado se rechaza con 409.

## Catálogos
- Estatus de proveedor: ACTIVO, INACTIVO, BLOQUEADO.
- Estatus de orden: BORRADOR, APROBADA, RECIBIDA_PARCIAL, RECIBIDA, CANCELADA.

## Entidades
- **Proveedor:** id (UUID), razon_social, rfc (cifrado, indexado con HMAC-SHA256), correo (cifrado), estatus, version.
- **OrdenCompra:** id (UUID), proveedor_id (UUID), estatus, total, moneda, version.
- **LineaOrden:** id (UUID), orden_id (UUID), articulo_sku, cantidad (BigDecimal), precio_unitario (BigDecimal).
- **Recepcion:** id (UUID), orden_id (UUID), cantidad_recibida (BigDecimal), fecha (LocalDate).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /proveedores | COMPRAS | 201 | 400, 409 |
| GET | /proveedores/{id} | COMPRAS, FINANZAS | 200 | 404 |
| POST | /ordenes-compra | COMPRAS | 201 | 400, 404 |
| POST | /ordenes-compra/{id}/aprobacion | DIRECCION | 200 | 404, 409 |
| POST | /ordenes-compra/{id}/recepciones | ALMACEN | 201 | 404, 409, 422 |
| POST | /ordenes-compra/{id}/cancelacion | COMPRAS | 200 | 400, 404, 409 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| orden-compra-aprobada | Aprobación | id, proveedor_id, total |
| insumos-recibidos | Recepción | orden_id, lineas |

## Requisitos funcionales
- Gestionar proveedores.
- Emitir y aprobar órdenes de compra.
- Registrar recepciones parciales o totales.

## Requisitos no funcionales
- Búsqueda de proveedores en menos de 300 ms para catálogos de hasta 10,000 registros (propuesta).

## Seguridad y acceso
- COMPRAS: alta y órdenes.
- DIRECCION: aprobación.
- ALMACEN: recepciones.
- Datos sensibles cifrados con AES-256-GCM.

## Observabilidad
- Métricas: ordenes_compra_total, recepciones_total, errores 5xx.
- Logs sin RFC, correo ni razón social.

## Criterios de aceptación
- Una recepción mayor a la cantidad ordenada devuelve 409.
- Un RFC duplicado devuelve 409.

## Pruebas
- Unitarias: estatus de orden y recepciones.
- Integración: PostgreSQL y Kafka con Testcontainers.
- Contrato: endpoints y eventos.

## Despliegue y rollback
Canary con criterio del anexo. Rollback por contenedor.

## Pendientes por integración futura
- Pago a proveedores desde Facturación y pagos.
- Portal de proveedores.

## Comentarios
- Se debe confirmar quién aprueba órdenes y el monto límite de aprobación.

## Roles en el área
- **COMPRAS:** gestiona proveedores y órdenes.
- **DIRECCION:** aprueba órdenes.
- **ALMACEN:** registra recepciones.
