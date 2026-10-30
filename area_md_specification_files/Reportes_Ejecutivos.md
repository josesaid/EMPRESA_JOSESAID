# Reportes ejecutivos

**Área:** Dirección general
**Servicio propuesto:** reportes-ejecutivos-ms

## Rol
Eres el microservicio de reportes ejecutivos. Consolidas indicadores de todas las áreas para la dirección.

## Contexto
**Incluye:** indicadores consolidados por área, tableros de consulta y exportación de reportes.
**Excluye:** la captura de datos (cada área es dueña de sus datos) y los cálculos contables (Contabilidad).

## Tarea
Recibir indicadores de cada área, consolidarlos por periodo y entregarlos a la dirección.

## Restricciones
- Los indicadores son solo de lectura para la dirección.
- Sin datos personales en reportes.

## Formato de salida
JSON para API; PDF y XLSX para exportación (propuesto).

## Escenarios de entrada
1. Evento de cambio en un indicador de otra área → actualización de la vista.
2. Solicitud de reporte por periodo.
3. Exportación del reporte mensual.

## Reglas de negocio (propuestas)
- Un indicador sin fuente reciente se marca como desactualizado.
- Un reporte cerrado no cambia; se genera uno nuevo.

## Catálogos
- Áreas: Ventas y atención a clientes, Finanzas y contabilidad, Operaciones, Compras y proveedores, Administración y legal, Dirección general.
- Periodos: mensual, trimestral, anual.

## Entidades
- **Indicador:** id (UUID), area, nombre, valor (BigDecimal), periodo, fuente_actualizada_utc (Instant).
- **Reporte:** id (UUID), periodo, estatus (BORRADOR, CERRADO), creado_utc (Instant).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| GET | /indicadores?area=&periodo= | DIRECCION | 200 | 400 |
| GET | /reportes/{periodo} | DIRECCION | 200 | 404 |
| POST | /reportes/{periodo}/cierre | DIRECCION | 200 | 404, 409 |
| GET | /reportes/{id}/exportacion?formato= | DIRECCION | 200 | 400, 404 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| reporte-cerrado | Cierre del reporte | id, periodo |

## Requisitos funcionales
- Consolidar indicadores de todas las áreas.
- Generar reportes por periodo.
- Exportar reportes.

## Requisitos no funcionales
- Consolidación mensual en menos de 2 minutos (propuesta).

## Seguridad y acceso
- DIRECCION: lectura completa.
- Cada área solo ve sus indicadores en el consolidado (propuesta).

## Observabilidad
- Métricas: indicadores_recibidos_total, reportes_cerrados_total, errores 5xx.

## Criterios de aceptación
- Un indicador desactualizado aparece marcado en el reporte.
- Un reporte cerrado devuelve 409 ante cambios.

## Pruebas
- Unitarias: consolidación y estado de frescura.
- Integración: PostgreSQL y Kafka con Testcontainers.

## Despliegue y rollback
Canary con criterio del anexo. Rollback por contenedor.

## Pendientes por integración futura
- Tableros interactivos.
- Alertas por umbral.

## Comentarios
- Este servicio depende de eventos de todas las áreas; conviene generarlo al final.

## Roles en el área
- **DIRECCION:** consulta y cierra reportes.
