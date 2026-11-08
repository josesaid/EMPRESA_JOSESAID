# ui-ms

Interfaz web de EMPRESA_JOSESAID. Renderiza las pantallas del sistema (cuentas, pólizas, acciones pendientes y cambio de contraseña) y consume las APIs a través del gateway.

## Rutas principales

| Ruta | Método | Para qué sirve |
|---|---|---|
| `/login` | GET | Pantalla de inicio de sesión |
| `/` | GET | Inicio |
| `/cuentas` | GET | Catálogo de cuentas |
| `/cuenta/cambiar-password` | GET, POST | Cambio de contraseña |
| `/polizas/nueva` | GET, POST | Captura de una póliza |
| `/acciones/{id}/confirmacion` | POST | Confirmar una acción pendiente |
| `/acciones/{id}/cancelacion` | POST | Cancelar una acción pendiente |

## Configuración

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto interno del servicio |
| `GATEWAY_URL` | `http://localhost:8080` | Dirección del gateway |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Caché de sesiones de la UI |
| `UI_PROXIES_CONFIABLES` | vacío | Proxies de confianza para `X-Forwarded-For` del navegador. Vacío usa la IP directa |
| `GATEWAY_TLS_BUNDLE` | vacío | Bundle de certificados si el gateway usa TLS interno |

## Ejecución

En desarrollo, con Docker Compose desde esta carpeta:

```bash
docker compose up -d --build
```

La imagen se construye con el contexto de la raíz del repositorio. Por eso el Compose usa `context: ..`.

La UI se publica solo en `127.0.0.1:8098`, así que no está expuesta en la red local.

## Seguridad

- Las sesiones se guardan en Redis.
- El detalle del flujo de inicio de sesión está en [`docs/seguridad-login.md`](docs/seguridad-login.md).
- El actuator de la UI no se publica. Queda pendiente de definir su exposición.

## Guías

- [Levantar el ecosistema completo](../docs/guias/01-levantar-ecosistema.html)
- [Ejecutar las pruebas de un servicio](../docs/guias/02-ejecutar-pruebas.html)
- [Índice de todas las guías](../docs/guias/README.html)
