# Diseño técnico: facturacion-pagos-ms

## 1. Contexto

El área de Facturación y pagos necesita emitir comprobantes a partir de órdenes, cobrarlos y cancelarlos con trazabilidad. Este documento describe las decisiones técnicas. Los requisitos funcionales están en `area_md_specification_files/Facturacion_Pagos.md`.

## 2. Objetivos

- Reglas de facturación protegidas en el dominio, sin depender de Spring ni de la base de datos.
- Ningún cambio de estado se pierde ni queda sin evento.
- Cada comprobante se puede auditar: nada se borra, las correcciones son compensaciones.
- Servicio independiente: arranca solo, con su propia base de datos.

## 3. Arquitectura en capas (DDD)

| Capa | Contenido | Depende de |
|---|---|---|
| Dominio | `Comprobante` (raíz), `Pago`, enums, excepciones y el puerto `ComprobanteRepositorio` | Nada del framework |
| Aplicación | `FacturacionService`, `EventoDominio`, puerto `PublicadorEventos` | Dominio |
| Infraestructura | Entidades JPA, adaptador, outbox y Kafka | Aplicación y dominio |
| API | Controlador REST, DTOs y `ProblemDetail` | Aplicación |

La regla de dependencia apunta hacia adentro: el dominio no conoce JPA, Kafka ni HTTP.

## 4. Agregado Comprobante

- **Raíz:** `Comprobante`. Los pagos son entidades internas que solo se crean a través de la raíz.
- **Invariantes:**
  - Un comprobante se crea en borrador con IVA y total calculados con redondeo bancario (`HALF_EVEN`).
  - Solo un borrador se emite, y al emitir se asigna el folio.
  - Solo un emitido se cancela, y la cancelación exige motivo.
  - Un pago es positivo, tiene fecha y no excede el saldo.
- **Estado de cobro:** se deriva de los pagos (`PENDIENTE`, `PARCIAL`, `PAGADO`). No se guarda, así no puede desincronizarse.

## 5. Persistencia

- PostgreSQL 17 con Flyway. El esquema lo controla la migración, y Hibernate solo lo valida.
- `Comprobante` usa control optimista (`@Version`) para evitar dos pagos concurrentes que excedan el saldo.
- Los pagos son inmutables: no se actualizan ni se borran. Las correcciones se harán con compensaciones en una versión posterior.
- El adaptador reconstruye el agregado desde la base de datos sin volver a validar las reglas.

## 6. Eventos y outbox

- Cada cambio de estado escribe el evento en `outbox_eventos` dentro de la misma transacción.
- Un retransmisor programado envía los eventos pendientes a Kafka, en orden, y se detiene en el primer fallo.
- La clave del mensaje es el id del comprobante, para que los eventos de un mismo comprobante queden en la misma partición y en orden.
- Consecuencia: los consumidores deben tolerar entrega al menos una vez (duplicados posibles).

## 7. Seguridad

- Tokens JWT HS256 emitidos por usuarios-accesos-ms. Este servicio solo valida firma y expiración.
- Autorización por rol en cada método. Escribir exige `FINANZAS`; leer también permite `VENTAS`.
- Secreto de al menos 32 bytes, sin valor por defecto. El servicio no arranca sin él.

## 8. Errores

Todos los errores siguen RFC 9457. Cada `type` es estable y tiene el prefijo `urn:josesaid:facturacion-pagos:error:`, para que los clientes puedan reaccionar sin leer el texto.

## 9. Operación

- Arranque después de PostgreSQL, Kafka y usuarios-accesos-ms (ver README, sección 6).
- Salud y métricas en `/actuator`. Logs JSON en formato ECS.
- Despliegue como imagen de contenedor sin privilegios.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| Folio provisional no válido fiscalmente | Se documenta como pendiente; el timbrado con PAC reemplaza la generación. |
| Pagos duplicados por reintentos del cliente | Pendiente `Idempotency-Key`. Mientras tanto, el cliente no debe reintentar sin consultar. |
| Eventos duplicados en Kafka | Los consumidores son idempotentes por `agregadoId` y tipo. |
| Expiración del token en un proceso largo | Los tokens duran 15 minutos. El cliente debe renovarlos. |

## 11. Decisiones pendientes

1. Integrar el PAC para timbrar y obtener el UUID fiscal.
2. Definir cancelación fiscal ante el SAT.
3. Añadir `Idempotency-Key` a creación y pagos.
4. Añadir trazas distribuidas y métricas de negocio.
