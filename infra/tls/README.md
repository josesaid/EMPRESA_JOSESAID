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

## Pendiente antes de activarlo en el entorno compartido

**`ui-ms` y `nexus-ms` llaman al gateway por `http://gateway-ms:8080`** (`GATEWAY_URL` en sus archivos de Compose). Si activas TLS en el gateway sin cambiarles la URL y darles la CA, dejan de poder llamarlo. Ese cambio es la segunda entrega (tráfico interno), y no está en esta PR.

Tampoco cambian todavía: Eureka, Prometheus (que raspa `gateway-ms:9080`, HTTP por la red interna) y los demás servicios.
