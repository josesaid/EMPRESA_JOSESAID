# Diseño de documentos-cumplimiento-ms

## Contexto

Administración y legal necesita guardar contratos, permisos y registros con historial de cambios, protección del contenido y reglas claras de retención. El servicio es dueño de los documentos y sus versiones. No es dueño de la lógica de negocio de cada área.

## Lenguaje ubicuo

- **Documento:** registro legal con metadatos y versiones.
- **Versión:** copia inmutable del contenido, con hash y número.
- **Retención:** periodo mínimo durante el que un documento no puede eliminarse.
- **Vencimiento:** fecha en que un permiso deja de ser válido.

## Agregado

**Documento** (raíz). Invariantes: un permiso tiene fecha de vencimiento; no se crea con vencimiento pasado; cada cambio de contenido crea una versión; un documento vencido o eliminado no recibe versiones; eliminar exige retención vencida y motivo.

## Decisiones

1. **Versiones inmutables en dos niveles.** El dominio nunca modifica una versión, y un trigger en PostgreSQL impide `UPDATE` y `DELETE` sobre la tabla.
2. **Hash antes de guardar.** El hash SHA-256 se calcula sobre el contenido original. Cada descarga lo vuelve a calcular y lo compara.
3. **Cifrado en reposo.** AES-256-GCM con IV aleatorio por archivo. La etiqueta de autenticación detecta cualquier alteración.
4. **Validar antes de escribir.** El dominio valida la nueva versión antes de guardar el archivo, para no dejar archivos huérfanos por reglas de negocio.
5. **Eliminación lógica.** El documento queda marcado con motivo y fecha. La purga física requiere política de Legal.
6. **Alertas una sola vez.** Un permiso emite `permiso-por-vencer` una vez. Una bandera en el documento evita repetirla.
7. **Referencia oculta.** La API nunca expone la ruta del archivo en disco.

## Seguridad

JWT RS256 con la clave pública de usuarios-accesos-ms (JWKS). Roles: LEGAL escribe y lee; DIRECCION solo consulta. Cada descarga y eliminación se registra en la auditoría sin contenido.

## Riesgos y pendientes

- Periodos de retención sin confirmación legal.
- Firma electrónica pendiente.
- Almacenamiento local; migración a nube pendiente.
- Purga física tras retención pendiente de política.

## Fuera de alcance

Firma electrónica, OCR, búsqueda de texto dentro de documentos y edición en línea.


## Indicadores para reportes

- **Indicador:** `documentos_por_vencer_30d` (área `ADMINISTRACION_LEGAL`). Documentos por vencer: no eliminados que vencen en los próximos 30 días; se publica solo el mes actual.
- **Publicación:** clase `PublicadorIndicadoresVencimientos`, cada 60 minutos, a `indicadores-areas`. Clave `ADMINISTRACION_LEGAL|documentos_por_vencer_30d|AAAA-MM`.
- **Decisión:** es una foto del periodo, no un evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Consumo:** `reportes-ejecutivos-ms` lo guarda con upsert por área, nombre y periodo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).
