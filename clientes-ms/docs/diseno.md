# Diseño de clientes-ms

## 1. Propósito

clientes-ms gestiona prospectos, clientes, contactos, interacciones con clientes y consentimientos de protección de datos. Lo consumen Pedidos, Facturación y la interfaz de ventas. Los diagramas están en [diagramas.md](diagramas.md).

## 2. Decisiones de arquitectura

- **Capas.** `domain` contiene entidades y reglas de estado. `application` contiene casos de uso, detección de duplicados y eventos. `infrastructure` contiene persistencia, cifrado y outbox. `api` contiene controladores, DTOs y errores. La API no conoce las entidades directamente: traduce a comandos y DTOs.
- **Entidades JPA en `domain`.** Las entidades llevan anotaciones JPA para reducir código de mapeo. Las reglas de transición viven en los enums y en métodos de la entidad, no en los controladores.
- **Outbox.** Cada evento se escribe en `outbox_evento` dentro de la misma transacción del cambio de estado. Un publicador programado lo envía a Kafka. Así no hay cambios sin evento ni eventos sin cambio.
- **Cifrado por columna.** RFC, correo, nombre y resumen de interacción se cifran con AES-256-GCM. Cada valor tiene un IV aleatorio.
- **Búsqueda sin texto plano.** Los campos `*_hash` guardan HMAC-SHA256 del valor normalizado. Sirven para buscar por RFC, correo o nombre y para detectar duplicados sin descifrar.
- **Fechas.** Los instantes se guardan en UTC. Las fechas de negocio sin hora son `LocalDate`. La conversión a `America/Mexico_City` ocurre solo en la capa `api`.
- **Errores.** Las respuestas siguen RFC 9457 con tipos `urn:clientes:error:<codigo>`.

## 3. Modelo de datos

| Tabla | Propósito | Restricción destacada |
|---|---|---|
| `prospecto` | Posible cliente | Estatus con `CHECK` |
| `cliente` | Cliente formal | Índice único parcial sobre `rfc_hash` donde `estatus <> 'BAJA'` |
| `contacto` | Persona de contacto del cliente | Índice único parcial: un solo `principal = TRUE` por cliente |
| `interaccion` | Historial de llamadas, correos y visitas | Índice por cliente y fecha descendente |
| `consentimiento` | Autorización por finalidad | Revocación con `revocado_at` |
| `outbox_evento` | Eventos pendientes de publicar | Índice parcial sobre pendientes |

Los diagramas de entidades y relaciones están en [diagramas.md](diagramas.md).

## 4. Estados

- **Prospecto:** `NUEVO → CONTACTADO → CALIFICADO → CONVERTIDO`. `PERDIDO` se alcanza desde cualquier estado abierto. `CONVERTIDO` solo se alcanza con la operación de conversión.
- **Cliente:** `ACTIVO ↔ SUSPENDIDO`. Cualquier estado pasa a `BAJA`. `BAJA` es terminal.
- Repetir el estatus actual es una operación idempotente y responde `200` sin cambios.

## 5. Endpoints

| Método | Ruta | Rol autorizado | Éxito | Errores |
|---|---|---|---|---|
| POST | `/v1/prospectos` | VENTAS | 201 | 400, 409 |
| GET | `/v1/prospectos/{id}` | VENTAS | 200 | 404 |
| PATCH | `/v1/prospectos/{id}/estatus` | VENTAS | 200 | 404, 409 |
| POST | `/v1/prospectos/{id}/conversion` | VENTAS, ADMIN | 201 | 404, 409 |
| POST | `/v1/clientes` | ADMIN | 201 | 400, 409 |
| GET | `/v1/clientes?rfc=&nombre=` | VENTAS, ADMIN | 200 | 422 |
| GET | `/v1/clientes/{id}` | VENTAS, ADMIN | 200 | 404 |
| PATCH | `/v1/clientes/{id}` | ADMIN | 200 | 400, 404 |
| PATCH | `/v1/clientes/{id}/estatus` | ADMIN | 200 | 404, 409 |
| POST | `/v1/clientes/{id}/contactos` | ADMIN | 201 | 400, 404 |
| POST | `/v1/clientes/{id}/interacciones` | VENTAS | 201 | 400, 404 |
| GET | `/v1/clientes/{id}/interacciones` | VENTAS | 200 | 404 |
| POST | `/v1/contactos/{id}/consentimientos` | LEGAL | 201 | 400, 404 |
| POST | `/v1/consentimientos/{id}/revocacion` | LEGAL | 200 | 404 |
| GET | `/v1/metricas/conversion?mes=yyyy-MM` | VENTAS | 200 | 400 |

## 6. Ejemplos de escritura

**Crear prospecto**

```json
{ "nombre": "Ana López", "correo": "ana.lopez@ejemplo.mx", "rfc": "LOPA800101AB1", "fuente": "CAMPANA" }
```

Respuesta `201`: el nombre, el correo y el RFC vienen enmascarados (`A***`, `a***@ejemplo.mx`, `LOP********B1`).

**Convertir prospecto**

```json
{
  "razonSocial": "Industrias Demo SA de CV",
  "rfc": "IDE900101XY1",
  "contactoPrincipal": { "nombre": "Luis Pérez", "correo": "luis@demo.mx", "rol": "COMPRAS" }
}
```

Respuesta `201` con el cliente en estatus `ACTIVO`.

**Registrar interacción**

```json
{ "tipo": "LLAMADA", "fecha": "2026-10-29T18:30:00Z", "resumen": "Seguimiento de propuesta." }
```

Respuesta `201`: la fecha se devuelve en `America/Mexico_City`, por ejemplo `2026-10-29T12:30:00-06:00`.

**Otorgar consentimiento**

```json
{
  "finalidad": "Comunicaciones comerciales",
  "fechaOtorgado": "2026-10-29T18:00:00Z",
  "medio": "FORMULARIO",
  "vigencia": "2027-10-29"
}
```

Respuesta `201`.

## 7. Eventos

| Evento | Disparador | Clave Kafka | Datos principales |
|---|---|---|---|
| `ClienteCreado` | Conversión de prospecto o alta directa | `clienteId` | `clienteId`, `contactoPrincipalId`, `estatus`, `prospectoId` (solo en conversión) |
| `ClienteEstatusCambiado` | Cambio de estatus de cliente | `clienteId` | `clienteId`, `estatusAnterior`, `estatusNuevo` |
| `ConsentimientoRevocado` | Primera revocación | `consentimientoId` | `consentimientoId`, `contactoId`, `finalidad` |

Todos los eventos incluyen `eventoId`, `tipoEvento` y `ocurridoEn`. Entrega al menos una vez. Los eventos no contienen datos personales.

## 8. Observabilidad

- **Métricas (Micrometer, Prometheus):** `clientes.altas` (etiqueta `tipo`), `clientes.conversiones`, `clientes.duplicados.detectados` (etiqueta `criterio`), y `http.server.requests` por endpoint.
- **Trazas (OpenTelemetry, OTLP HTTP):** muestreo al 100%. Ajustable con `management.tracing.sampling.probability`.
- **Logs:** formato ECS en JSON. Nunca se registran RFC, correo, nombre completo ni contenido de resúmenes. El registro de auditoría `auditoria.datos_personales` contiene operación, recurso y actor.

## 9. Pruebas

- **Unitarias (13):** enmascarado, transiciones de estatus, idempotencia de revocación y vigencia de consentimientos.
- **Integración (5, `*IT`):** flujo completo de conversión, RFC duplicado con `409`, cliente en `BAJA` no reactivable, y control de acceso por rol. Usan PostgreSQL 17 y Kafka reales con Testcontainers, y requieren Docker.
- **Contrato:** pendiente. El documento de especificación no define contratos con los consumidores.

Criterios de aceptación del documento de especificación:

- Conversión: un prospecto `CALIFICADO` con contacto principal válido pasa a cliente `ACTIVO` en una sola operación.
- Duplicados: no se pueden crear dos clientes no dados de baja con el mismo RFC.
- Interacciones: el historial de un cliente se devuelve en orden cronológico descendente.
- Consentimiento: al revocar, se emite `ConsentimientoRevocado`.

## 10. Despliegue y rollback

- **Despliegue gradual:** primero 10% del tráfico de la interfaz de ventas.
- **Criterio de éxito:** errores 5xx menores a 1% durante 30 minutos.
- **Migraciones:** Flyway, versionadas y compatibles hacia atrás.
- **Rollback de la aplicación:** volver a la imagen anterior. Las migraciones no se revierten; se corrigen con una nueva.
- **Rollback de operaciones:** una alta o conversión incorrecta se compensa con una baja (`BAJA`), que deja el registro para auditoría.

## 11. Pendientes

1. Confirmar el valor `CAMPANA` (la especificación dice `CAMPAÑA`). Se usó sin tilde para evitar caracteres no ASCII en los valores de base de datos.
2. Definir los valores definitivos de `MedioConsentimiento`.
3. Definir el plazo de retención de prospectos no convertidos.
4. Revisión legal del texto de consentimiento antes de producción.
5. Procedimiento de anonimización para derechos ARCO.
6. Confirmar si el control de concurrencia optimista (campo `version`) aplica también a clientes, además de prospectos.

## Indicadores para reportes

- **Indicador:** `clientes_nuevos_mes` (área `VENTAS_ATENCION_CLIENTES`). Clientes nuevos del mes: clientes dados de alta en el mes por fecha de alta, sin importar su estatus posterior.
- **Publicación:** clase `PublicadorIndicadoresClientes`, cada 60 minutos (mes actual y anterior), a `indicadores-areas`. Clave `VENTAS_ATENCION_CLIENTES|clientes_nuevos_mes|AAAA-MM`.
- **Decisión:** foto del periodo, no evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).
