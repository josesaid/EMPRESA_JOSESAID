# Piloto de Nexus por áreas — EMPRESA_JOSESAID

Fecha: 8 de noviembre de 2026

## Resumen

El piloto verificó 22 respuestas de Nexus en 5 áreas. Cada respuesta coincide con el dato que devuelve el servicio directamente.

Los datos de prueba se conservan en la base local, por indicación del usuario.

## Resultados por área

| Área | Preguntas | Resultado frente al servicio |
| --- | --- | --- |
| Compras y proveedores | P1 a P6 (6) | 6 de 6 coinciden |
| Finanzas | F1 a F5 (5) | 5 de 5 coinciden |
| Contabilidad | C1 a C4 (4) | 4 de 4 coinciden |
| Inventario | I1 a I3 (3) | 3 de 3 coinciden; I1 tras corregir el filtro |
| Documentos y cumplimiento | D1 a D4 (4) | 4 de 4 coinciden |
| **Total** | **22** | **22 de 22** |

Las consultas de compras, finanzas y documentos usaron el usuario de su rol (COMPRAS, FINANZAS y LEGAL). Contabilidad usó CONTADOR e inventario usó ALMACEN.

## Hallazgos

1. **Filtro de datos sensibles (corregido, PR #169).** El patrón de teléfono confundía grupos de dígitos dentro de UUID y los reemplazaba por `[TELEFONO]`. Las consultas con esos IDs fallaban. El arreglo está fusionado y verificado con I1.
2. **Límite diario de tokens.** El presupuesto por usuario (15000 por defecto) se agotó varias veces durante el piloto. Se subió a 150000 solo para las corridas y se regresó a 15000 al terminar.
3. **Fechas en UTC.** Nexus muestra fechas de creación en UTC. Por ejemplo, un documento creado a las 23:41 del 8 de noviembre (hora de Ciudad de México) aparece como 9/11.
4. **Errores de arranque.** Tras reconstruir `nexus-ms`, las primeras consultas devolvieron 503 mientras el servicio indexaba políticas. Hay que esperar a que el contenedor esté `healthy` antes de probar.

## Pendientes

- [ ] Corregir la fecha de Nexus para que use hora de Ciudad de México en negocio.
- [ ] Decidir si el presupuesto diario por usuario sube de forma permanente o se mantiene en 15000.
- [ ] Usuarios y accesos quedan fuera del alcance por ahora.

## Fuentes

Resultados verificados contra los endpoints de cada servicio en el stack local (`localhost:8080`), el 8 de noviembre de 2026. Los datos de prueba se conservan en la base local.
