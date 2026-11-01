# Conectar un cliente MCP a Nexus (piloto)

Nexus expone sus herramientas por MCP en `/mcp`. Cada llamada usa el mismo JWT que usa la plataforma, así que un usuario solo ve y puede hacer lo que su rol permite.

## 1. Obtener el token

```bash
curl -s -X POST http://<host>/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"tu.correo@empresa.com","password":"<tu contraseña>"}'
```

La respuesta trae `access_token` y `refresh_token`. El access token dura **15 minutos**.

## 2. Renovar el token

Cuando el access token venza, rota el refresh token:

```bash
curl -s -X POST http://<host>/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}'
```

Cada renovación devuelve un refresh token nuevo; guarda el último.

## 3. Configurar el cliente MCP

- URL del servidor: `http://<host>/mcp`
- Encabezado: `Authorization: Bearer <access_token>`

Cualquier cliente que acepte un servidor MCP remoto con encabezados personalizados sirve. Revisa la documentación de tu cliente para ese campo.

## 4. Herramientas disponibles

- `consultar_politicas`: busca políticas internas que tu rol puede ver, con la fuente de cada fragmento.
- `consultar_poliza`, `listar_polizas`, `consultar_estados_financieros`: lectura contable según tu rol.
- `sugerir_poliza`: propone una póliza. **No la registra.** Devuelve un id y un código de confirmación de 8 dígitos.
- `confirmar_accion`: confirma la acción con su id y su código. Solo funciona para quien la preparó y dentro de los 10 minutos de vigencia.
- `cancelar_accion`: cancela la acción sin ejecutarla.

Si el código es incorrecto, la acción se cancela por seguridad: hay que pedir una nueva propuesta.

## Notas del piloto

- El token se pega a mano, así que hay que renovarlo cada 15 minutos.
- El código de confirmación aparece en la respuesta de `sugerir_poliza`, dentro del cliente. Quien tenga acceso a esa conversación puede verlo; por eso la confirmación es una barrera suave.
- Para conectar en desarrollo sin gateway, usa `http://localhost:8081/mcp`.
