# Clientes (CRM)

**Área:** Ventas y atención a clientes
**Servicio propuesto:** clientes-ms

## Rol
Eres el microservicio de clientes (CRM). Gestionas prospectos, clientes, contactos e historial de interacciones.

## Contexto
**Incluye:** prospectos, clientes, contactos por cliente, historial de interacciones (llamadas, correos, visitas), consentimientos de contacto y detección de duplicados.
**Excluye:** cotizaciones, pedidos y facturas (Pedidos y ventas, Facturación y pagos), y la autenticación de usuarios (Usuarios y accesos).
**Consumidores:** Pedidos y ventas, Facturación y pagos, y la interfaz de ventas.

## Tarea
Registrar y actualizar prospectos y clientes, convertir prospectos en clientes, registrar interacciones y consultar el historial de cada cliente.

## Restricciones
- Cumplir la LFPDPPP para datos personales.
- No se almacenan datos de tarjeta.
- RFC y correo enmascarados en listados.
- Fechas en UTC; se convierten a `America/Mexico_City` solo en la capa de presentación.

## Formato de salida
JSON. Diagramas en SVG o Mermaid, conforme a crea-area-empresa.

## Escenarios de entrada
1. **Conversión:** un prospecto pasa a cliente al firmar contrato. El cliente debe tener un contacto principal con correo válido.
2. **Actividad:** ventas registra llamadas, correos y visitas con fecha, responsable y resumen. El historial se consulta por cliente.
3. **Cliente corporativo:** un cliente tiene tres contactos (compras, pagos y asistente), cada uno con comunicaciones distintas.
4. **Búsqueda:** un vendedor busca un cliente por RFC o nombre y ve su última interacción.
5. **Consentimiento:** cada prospecto registra su fuente (campaña, referido o evento) y el consentimiento de contacto.

## Reglas de negocio
- Un prospecto se convierte en cliente una sola vez.
- Prospecto: `NUEVO → CONTACTADO → CALIFICADO → CONVERTIDO`, o `PERDIDO`.
- Cliente: `ACTIVO ⇄ SUSPENDIDO`, y `BAJA` es definitiva; un cliente en `BAJA` no se reactiva.
- Un cliente tiene exactamente un contacto principal.
- No pueden existir dos clientes activos con el mismo RFC.

## Duplicados
- Coincidencia exacta de RFC: `409 Conflict`, con el identificador existente.
- Solo coincide el correo o un nombre similar: advertencia de posible duplicado; se requiere `?confirmarDuplicado=true`.

## Catálogos
- **Fuente:** `CAMPAÑA`, `REFERIDO`, `EVENTO`.
- **Rol de contacto:** `COMPRAS`, `PAGOS`, `ASISTENCIA`.
- **Tipo de interacción:** `LLAMADA`, `CORREO`, `VISITA`.
- **Medio de consentimiento:** por definir con el área legal.

## Entidades
- **Prospecto:** `UUID id`, `String nombre`, `String correo`, `Fuente fuente`, `EstatusProspecto estatus`, `LocalDate fechaRegistro`.
- **Cliente:** `UUID id`, `String razonSocial`, `String rfc`, `EstatusCliente estatus`, `UUID prospectoId`, `LocalDate fechaAlta`, `long version`.
- **Contacto:** `UUID id`, `UUID clienteId`, `String nombre`, `String correo`, `RolContacto rol`, `boolean principal`.
- **Interaccion:** `UUID id`, `UUID clienteId`, `TipoInteraccion tipo`, `Instant fecha`, `UUID responsableId`, `String resumen`.
- **Consentimiento:** `UUID id`, `UUID contactoId`, `String finalidad`, `Instant fechaOtorgado`, `MedioConsentimiento medio`, `LocalDate vigencia`.

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/v1/prospectos` | Vendedor | 201 | 400, 409 |
| POST | `/v1/prospectos/{id}/conversion` | Vendedor | 201 | 404, 409, 422 |
| POST | `/v1/clientes` | Administrador de clientes | 201 | 400, 409 |
| PUT | `/v1/clientes/{id}` | Administrador de clientes | 200 | 404, 409 |
| POST | `/v1/clientes/{id}/interacciones` | Vendedor | 201 | 400, 404 |
| GET | `/v1/clientes/{id}/interacciones` | Vendedor, Gerente | 200 | 404 |
| GET | `/v1/clientes?rfc=&nombre=` | Vendedor, Gerente | 200 | 400 |
| POST | `/v1/consentimientos/{id}/revocacion` | Responsable de protección de datos | 200 | 404, 409 |
| GET | `/v1/metricas/conversion?mes=2026-10` | Gerente de ventas | 200 | 400 |

Las escrituras aceptan `Idempotency-Key`. El control de concurrencia es optimista, con `version`.

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| `ClienteCreado` | Conversión o alta | id, prospectoId, estatus |
| `ClienteEstatusCambiado` | Cambio de estatus | id, estatusAnterior, estatusNuevo |
| `ConsentimientoRevocado` | Revocación | id, contactoId, finalidad |

Los eventos se publican con el patrón outbox.

## Requisitos funcionales
- Conversión de prospecto a cliente con validación de contacto principal y correo.
- Registro y consulta paginada de interacciones.
- Búsqueda por RFC o nombre.
- Métricas de conversión por mes.
- Registro y revocación de consentimientos.
- Detección de duplicados por RFC y correo.
- Anonimización para derechos ARCO, sin romper el historial.
- Retención definida para prospectos no convertidos.

## Requisitos no funcionales
- Identificador único y auditoría (creación y última modificación en UTC).
- Operaciones de estatus idempotentes.
- Consulta por identificador en menos de 300 ms (p95).
- Disponibilidad objetivo de 99.9%.
- Datos personales cifrados en reposo; TLS en tránsito.

## Seguridad y acceso
- OAuth2/JWT emitido por Usuarios y accesos.
- Autorización por rol (ver tabla de roles).
- Auditoría de accesos a datos personales.

## Observabilidad
- Métricas: altas, conversiones, duplicados detectados y latencia por endpoint (Micrometer + Prometheus).
- Trazas con OpenTelemetry y correlación entre servicios.
- Logs JSON estructurados.
- Nunca se registra: RFC, correo, nombre completo ni el contenido del resumen.

## Criterios de aceptación
- Un prospecto con contacto principal válido se convierte en cliente `ACTIVO` en una sola operación.
- El sistema rechaza un segundo cliente activo con el mismo RFC.
- Las interacciones de un cliente aparecen en orden cronológico descendente.
- Al revocar un consentimiento, no se envían más comunicaciones de esa finalidad.

## Pruebas
- Unitarias: transiciones de estado, duplicados y reglas de contacto principal.
- Integración: PostgreSQL y Kafka con Testcontainers, incluido el outbox.
- Contrato: endpoints y eventos.

## Despliegue y rollback
- Canary con 10% del tráfico de ventas. Criterio del anexo: 5xx menor a 1% durante 30 minutos.
- Migraciones con Flyway, compatibles hacia atrás.
- Rollback por contenedor. Las correcciones son compensaciones, nunca borrados.

## Pendientes por integración futura
- Integración con Pedidos y ventas (consulta de `clienteId`).
- Integración con Facturación y pagos (datos fiscales del cliente).

## Comentarios
- Mantener el modelo simple al inicio; la segmentación comercial se agrega después.
- Revisar con el área legal el texto de consentimiento antes de producción.
- Definir si el `Idempotency-Key` se conserva por 24 horas.

## Roles en el área
- **Vendedor:** registra prospectos e interacciones; consulta clientes.
- **Gerente de ventas:** consulta métricas e historial.
- **Administrador de clientes:** da de alta y actualiza clientes y contactos.
- **Responsable de protección de datos:** gestiona consentimientos y solicitudes ARCO.
