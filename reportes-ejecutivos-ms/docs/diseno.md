# Diseño de reportes-ejecutivos-ms

## Contexto

Dirección general necesita ver, por periodo, los indicadores de todas las áreas, saber cuáles están al día y cerrar reportes que no cambien después. El servicio no captura datos de negocio: consolida lo que las áreas publican.

## Lenguaje ubicuo

- **Indicador:** valor de un área, con nombre, periodo y fecha de su fuente.
- **Periodo:** mensual, trimestral o anual.
- **Desactualizado:** indicador cuya fuente no se actualizó dentro del umbral.
- **Reporte:** consolidado de un periodo, en borrador o cerrado.
- **Foto:** copia de los indicadores tomada al cerrar el reporte.

## Agregados

**Reporte** (raíz). Invariantes: un solo reporte por periodo; no se cierra sin indicadores; un reporte cerrado no cambia ni se vuelve a cerrar.

**Indicador.** Invariantes: área, nombre, valor, periodo y fecha de fuente son obligatorios. Se identifica por área, nombre y periodo.

## Decisiones

1. **Consolidado en vivo, foto al cierre.** Mientras el reporte está en borrador, cada consulta calcula el consolidado. Al cerrar, se guarda una copia que nunca cambia.
2. **Doble protección del cierre.** El dominio rechaza cambios en un reporte cerrado. Además, un trigger en PostgreSQL impide actualizar o borrar el reporte cerrado y su foto.
3. **Frescura calculada al consultar.** Así un indicador que envejece aparece marcado sin necesidad de un proceso adicional. La foto guarda la marca del momento del cierre.
4. **Entrada por evento, con carga manual.** Las áreas publican por Kafka. La carga manual existe para correcciones y pruebas.
5. **Descarte de mensajes inválidos.** Un mensaje malformado o fuera de regla se registra y se descarta, para no bloquear el tópico. La cola de fallidos queda pendiente.
6. **Exportación por puerto.** `ExportadorReporte` define el contrato en la capa de aplicación. CSV, PDF (OpenPDF) y XLSX (Apache POI) son adaptadores en infraestructura. Agregar un formato nuevo no toca el servicio.

## Seguridad

JWT HS256 con el secreto compartido. Toda la API es de DIRECCION. Los datos no incluyen información personal.

## Riesgos y pendientes

- Cada área no ve solo sus indicadores todavía.
- Sin cola de mensajes fallidos.
- Nombre del tópico y umbral de frescura por confirmar.

## Fuera de alcance

Tableros interactivos, alertas por umbral, captura de datos de áreas y cálculos contables.
