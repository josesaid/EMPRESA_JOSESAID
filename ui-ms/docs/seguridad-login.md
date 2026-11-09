# Seguridad y acceso de ui-ms

## Flujo de acceso

1. El usuario abre cualquier página de `ui-ms`. Si no tiene sesión, Spring Security lo redirige a `/login`. Esto ocurre también en ventanas privadas, porque cada navegador tiene su propia sesión.
2. Al enviar el formulario, `ui-ms` llama a `POST /usuarios-accesos-ms/v1/auth/login` a través del gateway, con `correo` y `password`.
3. Si las credenciales son correctas, el servidor guarda el `accessToken` y el `refreshToken` en la sesión. El navegador solo recibe la cookie `JSESSIONID` (`HttpOnly`); nunca ve los tokens.
4. Los roles se leen del claim `roles` del JWT y se convierten en autoridades `ROLE_*`. Esto solo sirve para mostrar u ocultar menús: el backend valida firma y permisos en cada llamada.
5. Al cerrar sesión (`POST /logout`), se invalida la sesión y se regresa a `/login?salir`.

## Respuestas esperadas

| Caso | Resultado |
|---|---|
| Credenciales correctas | Redirige a `/` |
| Contraseña incorrecta | Redirige a `/login?error` con el mensaje de error |
| Sin sesión, entra a `/` | Redirige a `/login` |
| Cierra sesión | Redirige a `/login?salir` |

## Renovación del token

`ServicioSesion.accessTokenVigente()` devuelve un access token vigente. Si faltan menos de 60 segundos para que expire (el access token dura 15 minutos), llama a `POST /usuarios-accesos-ms/v1/auth/refresh` y actualiza los tokens de la sesión. Las llamadas a las APIs deben usar este método.

## Piezas principales

- `config/SeguridadConfig`: reglas de acceso, formulario de login y logout.
- `config/ClientesConfig`: cliente HTTP hacia el gateway (`ui.gateway-url`).
- `seguridad/UsuariosAuthenticationProvider`: valida credenciales contra `usuarios-accesos-ms`.
- `seguridad/ServicioSesion`: entrega el token vigente y lo renueva.
- `seguridad/RolesJwt`: extrae los roles del payload del JWT.

## Configuración

- `GATEWAY_URL`: en local `http://localhost:8080`; en Docker `http://gateway-ms:8080` (ya está en `docker-compose.yml`).
- `SERVER_PORT`: puerto de la interfaz (8098 en local).

## Pendientes

- Aún no hay protección contra fuerza bruta en el formulario de login.
- Falta una prueba automatizada del flujo de login.

## Diagramas

Los diagramas del flujo (arquitectura, login, renovación con lock, reintento tras 401 y cierre) están en [docs/diagramas.md](diagramas.md).

## Sesiones

Las sesiones se guardan en Redis (`spring-boot-starter-session-data-redis`).
