# Guía 10. Rastrear un request en Jaeger

**Objetivo:** seguir un request desde que entra por el gateway hasta cada servicio que lo atiende, para saber por dónde pasó y en qué paso se tardó o falló.

**Interfaz:** http://localhost:16686 (Jaeger).

## Antes de empezar

- macOS con **Docker Desktop abierto**.
- El ecosistema levantado. Jaeger se levanta junto con el resto con `start.sh` (ver [guía 1](01-levantar-ecosistema.md)).
- Un usuario con acceso. Si no tienes uno, pide que te den de alta ([guía 5](05-dar-de-alta-usuario.md)).
- Las trazas se guardan **en memoria**. Si reiniciaste el contenedor de Jaeger, las trazas anteriores ya no existen: genera el request de nuevo.

## Pasos

1. **Verifica que Jaeger responda.** Abre http://localhost:16686 en el navegador. Desde la terminal:

   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:16686
   ```

   Debe responder `200`.

2. **Inicia sesión y guarda el token** (vence en 15 minutos):

   ```bash
   curl -s -X POST http://localhost:8080/usuarios-accesos-ms/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"correo":"TU_CORREO","password":"TU_CONTRASEÑA"}'
   ```

   Copia el valor de `accessToken` y guárdalo:

   ```bash
   export TOKEN="PEGA_EL_ACCESS_TOKEN"
   ```

3. **Haz un request al servicio que quieres rastrear.** Anota la hora aproximada para ubicarlo después:

   ```bash
   date
   curl -s -i http://localhost:8080/clientes-ms/v1/clientes \
     -H "Authorization: Bearer $TOKEN"
   ```

4. **Busca la traza en Jaeger.**
   1. En **Service** elige `gateway-ms`. Todo request pasa primero por el gateway.
   2. Pulsa **Find Traces**.
   3. Localiza la traza de la hora que anotaste en el paso 3.

5. **Lee la traza.** Cada barra horizontal es un **span**, es decir, un paso del request:
   - El gateway aparece primero, y debajo de él el servicio que lo atendió (`clientes-ms`).
   - El ancho de cada barra es el tiempo que tardó ese paso.
   - Pulsa un span para ver su duración, su estado y sus etiquetas.

6. **Rastrea otro servicio si lo necesitas.** Cambia **Service** por el nombre del servicio, por ejemplo `usuarios-accesos-ms` para ver el login. Los nombres de los servicios coinciden con sus carpetas del repositorio.

7. **Identifica el cuello de botella.** Si un span tarda mucho más que los demás, ese servicio (o la base de datos que consulta) es donde conviene revisar.

## Cómo interpretar lo que ves

| Lo que ves en Jaeger | Qué significa | Qué revisar |
|---|---|---|
| Solo aparece `gateway-ms` | El request no llegó al servicio. | El código de respuesta del `curl`. Un `401` indica token vencido o inválido. |
| Un span aparece en rojo | Ese paso terminó con error. | Pulsa el span y revisa su etiqueta de error y los logs del servicio. |
| Un span dura mucho más que los demás | Ese paso es lento. | Revisa ese servicio con `docker compose logs <servicio>`. |

## Problemas comunes

- **No aparece ninguna traza.**
  - Revisa que Jaeger esté arriba: `docker compose -f infra/trazabilidad/docker-compose.yml ps`.
  - Revisa que elegiste el servicio correcto en **Service**.
  - Si corres un servicio fuera de Docker, define `TRAZAS_OTLP_ENDPOINT=http://localhost:4318/v1/traces` antes de arrancarlo.
- **La traza desapareció después de un rato o de reiniciar.** Es normal: las trazas viven en memoria de Jaeger. Vuelve a hacer el request.
- **El `curl` responde `401`.** El token venció a los 15 minutos. Vuelve al paso 2.
