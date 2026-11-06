# Auditoría de cobertura del cifrado de campos

Fecha: 2026-11-05. Revisión de las entidades JPA de cada servicio buscando campos con datos personales o fiscales, y comprobando si están cifrados.

Cómo se cifra en cada servicio:

- **clientes-ms, usuarios-accesos-ms**: `@Convert(converter = CampoCifradoConverter.class)` en la entidad o el dominio. Índices de búsqueda con HMAC (`*_hash`, `*_indice`).
- **compras-proveedores-ms**: columnas `*_cifrado` que escribe `ProtectorDatos`, más índice HMAC (`rfc_indice`).

## Campos cifrados

| Servicio | Campo | Estado |
|---|---|---|
| clientes-ms | `cliente.rfc` | Cifrado, con índice HMAC |
| clientes-ms | `prospecto.nombre`, `prospecto.correo`, `prospecto.rfc` | Cifrados, con índices HMAC |
| clientes-ms | `contacto.nombre`, `contacto.correo` | Cifrados |
| clientes-ms | `interaccion.resumen` | Cifrado |
| usuarios-accesos-ms | `usuario.correo`, `usuario.nombre_completo` | Cifrados, con índice HMAC (`correo_indice`) |
| compras-proveedores-ms | `proveedor.rfc_cifrado`, `proveedor.correo_cifrado` | Cifrados, con índice HMAC (`rfc_indice`) |

## Campos sin cifrar que requieren decisión

| Servicio | Campo | Por qué puede ser dato personal | Opción |
|---|---|---|---|
| clientes-ms | `cliente.razon_social` | Es personal cuando el cliente es persona física con actividad empresarial | **Decidido: dejar en claro** |
| nexus-ms | `accion_pendiente.resumen` | Guardaba nombre y correo del prospecto | **Resuelto (PR #125):** solo guarda la fuente. Queda en claro el `payload` de la misma acción, que necesita nombre y correo para crear el prospecto |
| documentos-cumplimiento-ms | `documento.titulo` | Puede incluir el nombre de una persona (por ejemplo, un contrato) | **Decidido: dejar en claro.** Falta definir la validación que evite nombres en el título |

## Fuera del alcance de esta revisión

- `pedidos-ventas-ms`, `inventario-operaciones-ms` y `contabilidad-ms`: no se revisó si hay campos personales en descripciones o notas libres. Conviene revisarlo antes de cerrar este punto.
- `facturacion-pagos-ms`: el campo `folio` no es dato personal. No se encontró RFC de receptor en las entidades.

## Próximo paso

Pendiente: decidir si el `payload` de las acciones de nexus se cifra, y definir la validación del título de documentos. Los demás campos ya tienen decisión.
