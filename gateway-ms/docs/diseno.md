# Diseño de gateway-ms

## Contexto

Los servicios de EMPRESA_JOSESAID necesitan encontrarse sin direcciones fijas y exponerse por un único punto de entrada. Este servicio resuelve una parte de ese problema.

## Decisiones

1. **Eureka para el registro.** Es el estándar de Spring Cloud para Spring Boot 4.x y no requiere otro componente de infraestructura.
2. **Rutas automáticas en el gateway.** El locator de discovery crea una ruta por cada servicio registrado; no hay que mantener rutas a mano.
3. **Sin validación de tokens en el gateway.** Cada servicio sigue validando su JWT, como antes. Así no hay un punto único de falla de seguridad.

## Límites y pendientes

- Sin autenticación en el panel de Eureka.
- Sin límites de tasa ni CORS.
- Centralizar la validación de JWT queda pendiente de decisión.

## Fuera de alcance

Malla de servicios, balanceo avanzado y trazabilidad distribuida (esta última es un contenedor aparte, pendiente).
