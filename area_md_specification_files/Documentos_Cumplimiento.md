# Documentos y cumplimiento

**Área:** Administración y legal
**Servicio propuesto:** documentos-cumplimiento-ms

## Rol
Eres el microservicio de documentos legales. Guardas contratos, permisos y registros legales con control de versiones y retención.

## Contexto
**Incluye:** almacenamiento de documentos, versiones, vencimientos de permisos, retención y registros de cumplimiento.
**Excluye:** firma electrónica (pendiente) y la lógica de negocio de cada área.

## Tarea
Guardar documentos con versión, registrar vencimientos y aplicar reglas de retención.

## Restricciones
- Una versión publicada no se modifica.
- Los documentos se almacenan cifrados.
- No se eliminan documentos dentro del periodo de retención.

## Formato de salida
JSON para metadatos; el archivo se entrega como descarga.

## Escenarios de entrada
1. Carga de contrato → versión 1.
2. Carga de nueva versión del mismo documento.
3. Vencimiento próximo de un permiso → alerta.
4. Vencimiento del periodo de retención → marcado para revisión.

## Reglas de negocio (propuestas)
- Cada cambio de contenido crea una nueva versión.
- Un documento vencido no se reactiva; se registra uno nuevo.
- Eliminar requiere que haya vencido el periodo de retención y motivo.

## Catálogos
- Tipos de documento: CONTRATO, PERMISO, REGISTRO_LEGAL.
- Periodos de retención por tipo (por definir).

## Entidades
- **Documento:** id (UUID), tipo, titulo, area, vence_en (LocalDate), retencion_hasta (LocalDate), version_actual, version.
- **VersionDocumento:** id (UUID), documento_id (UUID), numero, hash_sha256, almacenamiento_ref, creado_utc (Instant).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /documentos | LEGAL | 201 | 400 |
| POST | /documentos/{id}/versiones | LEGAL | 201 | 404, 409 |
| GET | /documentos/{id} | LEGAL, DIRECCION | 200 | 404 |
| GET | /documentos/{id}/versiones/{n}/descarga | LEGAL | 200 | 404 |
| GET | /documentos/por-vencer?dias= | LEGAL, DIRECCION | 200 | 400 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| permiso-por-vencer | Vencimiento dentro del umbral | documento_id, vence_en |
| documento-version-creada | Nueva versión | documento_id, numero |

## Requisitos funcionales
- Versionado de documentos.
- Alertas de vencimiento.
- Retención controlada.

## Requisitos no funcionales
- Cifrado en reposo de todos los archivos.

## Seguridad y acceso
- LEGAL: escritura y lectura.
- DIRECCION: solo lectura.
- Acceso a documentos personales auditado.

## Observabilidad
- Métricas: documentos_creados_total, versiones_creadas_total, errores 5xx.
- Logs sin contenido de documentos.

## Criterios de aceptación
- Cargar una nueva versión incrementa el número en uno.
- Eliminar dentro de retención devuelve 409.

## Pruebas
- Unitarias: versiones, retención, vencimientos.
- Integración: PostgreSQL, almacenamiento de archivos y Kafka con Testcontainers.

## Despliegue y rollback
Canary con criterio del anexo. Rollback por contenedor.

## Pendientes por integración futura
- Firma electrónica.
- Almacenamiento en la nube (proveedor por definir).

## Comentarios
- Se debe confirmar el periodo de retención legal aplicable.

## Roles en el área
- **LEGAL:** gestiona documentos.
- **DIRECCION:** consulta.
