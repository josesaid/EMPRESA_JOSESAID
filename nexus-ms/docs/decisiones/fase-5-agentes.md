# Fase 5 · Agentes (opcional)

**Decisión (paso 35): no crear agentes por dominio.** Paso 36 no aplica.

## Motivos

- Las herramientas de F1 a F3 son consultas y propuestas de un solo paso. Un agente no aporta lógica que el asistente único no tenga.
- El asistente único ya filtra herramientas por rol (`AlcanceHerramientas`) y limita las llamadas por pregunta (`max-total-tool-calls`).
- Las escrituras (pedidos, pólizas, cotizaciones) requieren confirmación humana con código o con la sesión del usuario. Un agente autónomo no cambia esa barrera.
- Cada agente multiplica el consumo de tokens. El presupuesto diario y mensual por usuario y el tope del servicio ya acotan el gasto; con agentes, la misma pregunta gastaría más.

## Cuándo reconsiderar

- Si las preguntas de cruce entre dominios llegan con frecuencia al límite de llamadas por pregunta.
- Si aparece un flujo de varios pasos que requiera razonamiento propio y no solo consultas.

Mientras no ocurra, la decisión se mantiene.

## Cobertura

Matriz de roles en `MatrizRolesHerramientasTest`: fija qué puede usar cada rol, incluido un usuario con dos roles y roles no mapeados.
