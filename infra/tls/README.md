# TLS del gateway (CA interna)

Primera entrega: el gateway puede servir HTTPS. **Desactivado por defecto**; no cambia nada mientras no se active.

## 1. Generar la CA y el certificado (una vez)

```bash
bash infra/tls/generar-certificados.sh
```

Crea en `infra/tls/out/` (ignorado por Git):

- `ca.crt`: certificado público de la CA. Los clientes deben confiar en él.
- `ca.key`: clave privada de la CA. Guárdala fuera de tu máquina de trabajo; quien la tenga puede firmar certificados.
- `gateway.p12`: keystore del gateway.
- `gateway-password.txt`: contraseña del keystore, si no definiste `GATEWAY_TLS_PASSWORD`.

El certificado del gateway vale 365 días y la CA 5 años (variables `TLS_DIAS_GATEWAY` y `TLS_DIAS_CA`). Tiene como nombres `localhost`, `gateway-ms` y `127.0.0.1`.

## 2. Activar TLS en el gateway

Variables de entorno para `gateway-ms`:

| Variable | Valor |
|---|---|
| `GATEWAY_TLS_ACTIVO` | `true` |
| `GATEWAY_TLS_KEYSTORE` | Ruta dentro del contenedor al `gateway.p12` |
| `GATEWAY_TLS_PASSWORD` | Contraseña del keystore (en `.env`, nunca en el repositorio) |

Con TLS activo, el gateway responde en `https://localhost:8080`.

## 3. Probar desde tu Mac

```bash
curl --cacert infra/tls/out/ca.crt https://localhost:8080/actuator/health   # 404: /actuator no se expone en 8080
curl --cacert infra/tls/out/ca.crt -X POST https://localhost:8080/usuarios-accesos-ms/v1/auth/login \
  -H "Content-Type: application/json" -d '{"correo":"x","password":"x"}'      # 400 o 401: TLS funciona
```

## 4. Activar TLS entre servicios (ui-ms y nexus-ms)

Ya no hace falta cambiar código. Con la superposición `docker-compose.tls.yml`, el gateway sirve HTTPS y `ui-ms` y `nexus-ms` llaman al gateway por `https://gateway-ms:8080`, confiando en la CA interna mediante un bundle SSL de Spring (`GATEWAY_TLS_BUNDLE=gateway`).

```bash
docker compose -f docker-compose.yml -f ui-ms/docker-compose.yml -f infra/tls/docker-compose.tls.yml up -d
```

Requisitos: haber ejecutado el paso 1 (`truststore.p12` y `gateway.p12` en `infra/tls/out/`) y tener `GATEWAY_TLS_PASSWORD` en `.env`.

Sin la superposición, todo sigue en HTTP como antes: el bundle SSL solo se usa cuando `GATEWAY_TLS_BUNDLE` tiene nombre.

**Pendiente de verificación:** esta parte no se probó en un entorno con Docker. Antes de dar por buena la activación, revisa en tu Mac que `ui-ms` inicie sesión, que `nexus-ms` responda a una consulta y que `https://localhost:8080` responda con `--cacert`.

Lo que no cambia: Eureka, Prometheus (`gateway-ms:9080`, HTTP dentro de `empresa-net`) y los servicios que no llaman al gateway. Los demás servicios de área no llaman al gateway, así que no se tocan en esta entrega.
