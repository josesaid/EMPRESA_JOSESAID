# Pendientes de documentación y calidad

Revisión al 2026-11-05.

## 1. Guías paso a paso con imágenes y diagramas

**Hallazgo.** La documentación tiene pocas guías de procedimiento:

- **Rotación de la clave RS256:** paso a paso en `usuarios-accesos-ms/README.md`, sección «Rotación de la clave RS256». Sin imágenes ni diagramas.
- **Conexión de un cliente MCP:** `nexus-ms/docs/conexion-mcp.md`. Sin imágenes ni diagramas.
- **Arranque del ecosistema y orden de arranque:** en el README de la raíz, en prosa.
- **Procedimientos pendientes de definir:** anonimización por derechos ARCO (`clientes-ms`) y rotación de la clave de cifrado de compras (`compras-proveedores-ms`).

**Pendiente.** Cada procedimiento debe tener una guía completa, con pasos numerados, capturas o imágenes y diagramas. Desde el README de cada servicio hay que enlazar a la página HTML correspondiente, que se publica en `docs/`. Guías a crear:

- [x] Levantar el ecosistema completo (`docs/guias/`) y verificar que está arriba (imágenes y diagrama del orden de arranque)
- [x] Ejecutar las pruebas de un servicio (`docs/guias/`) (`ejecutar-pruebas-<servicio>.sh`)
- [x] Rotar la clave RS256 sin cortar sesiones (`docs/guias/`) (dos reinicios)
- [x] Conectar un cliente MCP a nexus-ms (`docs/guias/`)
- [x] Dar de alta un usuario y asignarle rol y área (`docs/guias/`)
- [x] Crear un proveedor, una orden de compra, aprobarla y registrar la recepción (`docs/guias/`)
- [x] Registrar un pago a proveedor contra una cuenta por pagar (`docs/guias/`)
- [x] Emitir un comprobante, registrar un cobro y cancelar (`docs/guias/`)
- [ ] Procesar una solicitud de derechos ARCO (pendiente de definir el procedimiento)
- [ ] Rotar la clave de cifrado y la clave HMAC de compras (pendiente de diseño)
- [x] Consultar indicadores para reportes ejecutivos (`docs/guias/`)

**Total: 11 guías. 9 escritas en `docs/guias/` (2026-11-05); 2 pendientes de diseño (ARCO y rotación de clave de compras).** Hoy existen 2 guías parciales (RS256 y MCP) y 0 con imágenes o diagramas.

## 2. Calidad de código y seguridad

**Hallazgo.** Revisé los `pom.xml` y la raíz del repositorio:

- Plugins de Maven en uso: `spring-boot-maven-plugin`, `maven-failsafe-plugin`, `maven-compiler-plugin` (en `nexus-comun`) y `maven-dependency-plugin` (en `nexus-ms`).
- No hay PMD, Checkstyle, SonarQube, JaCoCo (cobertura), SpotBugs, OWASP Dependency-Check ni `maven-enforcer-plugin`.
- No hay configuración de CI (`.github/workflows` ni equivalente).

**Pendiente.**

- [x] Decidir qué herramientas adoptar (decisión parcial: JaCoCo 60%; Sonar fuera de esta fase):
  - [x] JaCoCo 0.8.12 en modo reporte: umbral de 60% de líneas a nivel bundle, sin romper el build (`haltOnFailure=false`).
  - [x] Checkstyle, PMD (con CPD) y SpotBugs en `verify`, en modo reporte (`failOnViolation=false`).
  - [x] OWASP Dependency-Check en el perfil `seguridad-dependencias` (`mvn verify -Pseguridad-dependencias`), porque descarga la base NVD y tarda. Falla solo con CVSS 11 o superior, y en modo reporte no corta el build.
  - [ ] Verificar el build con `mvn verify` en cada módulo. No se pudo correr en el entorno de trabajo. PMD y Checkstyle para estilo y errores comunes, JaCoCo para cobertura, SpotBugs para errores de seguridad en el bytecode, SonarQube o SonarCloud para el panel, y OWASP Dependency-Check para vulnerabilidades en dependencias.
- [ ] Definir umbrales de cobertura y de hallazgos por servicio.
- [ ] Revisar aspectos de seguridad en los microservicios:
  - [ ] Vulnerabilidades en dependencias y versiones de Spring Boot y de librerías.
  - [ ] Secretos: `.env` fuera del repositorio, claves de cifrado y de firma, y escaneo de secretos en commits.
  - [x] Exposición de `/actuator/prometheus` en los puertos del host: verificado el 2026-11-05. El gateway responde `404` en `/actuator` y en `/{servicio}/actuator`, y los servicios no publican su puerto. La UI (`ui-ms`) se publicó solo en `127.0.0.1:8098`.
  - [ ] Cobertura del cifrado de datos personales y fiscales, y que los logs no contengan PII ni montos.
  - [ ] Validación de entrada y límites de tamaño en las APIs.
  - [ ] Limitación de intentos en el gateway, además del bloqueo por cuenta.
  - [ ] Imágenes de contenedor: usuario no privilegiado, imagen base actualizada y escaneo de imágenes.
  - [ ] TLS entre servicios y fuera de la red de Docker.
  - [ ] Procedimiento de rotación y respaldo de claves, con su prueba.

**Decisiones tomadas por Said Olano (2026-11-05):**

- JaCoCo con mínimo de cobertura del 60%. Se empieza en modo de reporte antes de exigirlo en el build.
- SonarQube/SonarCloud queda fuera de esta fase. Se conserva como herramienta opcional para revisar más adelante, sin decisión pendiente.
- Bloqueo de cuenta de 5 minutos tras 5 intentos fallidos consecutivos (antes 15 minutos, propuesta no validada). Decisión cerrada; el equipo no volverá a decidir este punto. Aplica al backend (`usuarios-accesos-ms`) y a la UI (`ui-ms`). La vigencia del access token (15 minutos) no cambia.

**Total: 7 decisiones de herramientas y 9 aspectos de seguridad por evaluar.**

**Avances del 2026-11-05.** Los siguientes puntos están parcialmente resueltos. Sus casillas siguen abiertas porque el alcance original incluye más trabajo:

- Límites de tamaño y validación de entrada (#102 a #108): `clientes-ms` y los parámetros de búsqueda de todos los servicios tienen longitud máxima. El gateway limita el cuerpo a 64 KiB, salvo la carga de documentos (21 MB) (#103). Parámetros demasiado largos responden 400, con prueba en los seis servicios (#118 a #122). Falta el límite de cuerpo en los servicios, que queda como defensa en profundidad opcional.
- Escaneo de secretos (#100): gitleaks corre en push a `main` y en cada PR. Faltan las claves de firma.
- Imágenes de contenedor (#101): Trivy escanea las 13 imágenes con parches disponibles. Todas usan usuario no root. Los digests de las imágenes base están fijados (#110, #111).
- Rotación de claves: la guía de prueba en Mac y el inventario quedaron documentados (#94 a #97). Falta documentar respaldo y restauración, y el paso de re-cifrado interrumpido.

**Resultados de `mvn verify` (2026-11-06).** Corrido en los 13 módulos con `-DskipITs` en 12 de ellos. `clientes-ms` se corrió completo: 27 pruebas unitarias pasan, pero dos pruebas de integración (`PublicadorIndicadoresClientesIT`) requieren Docker y fallan en el entorno de trabajo, que no tiene Docker. Gateway pasa completo, incluido JaCoCo.

- Pruebas unitarias: pasan en los 13 módulos (203 en `nexus-ms`, 53 en `usuarios-accesos-ms`, 37 en `compras-proveedores-ms`, entre otros).
- PMD: 2 hallazgos en `gateway-ms`, `compras-proveedores-ms`, `facturacion-pagos-ms` y `usuarios-accesos-ms`; 12 en `nexus-ms`. Checkstyle y PMD están en modo reporte.
- SpotBugs: en modo reporte. Los hallazgos más comunes son `EI_EXPOSE_REP2` (exposición de referencias mutables), que en Spring suele ser falso positivo, y `CT_CONSTRUCTOR_THROW` en `CryptoConfig`. Queda para revisar caso por caso.
- Pendiente: correr las pruebas de integración (`failsafe`) en un entorno con Docker.

**Avances posteriores del 2026-11-05 (PR #109 a #125).**

- Logs: sin montos de indicadores y sin mensajes de excepción; solo se registra el nombre de la clase (#112, #113).
- IP real en el login: el gateway y la UI confían en `X-Forwarded-For` solo desde proxies configurados (#114, #115, #116). Falta activarlo en el Mac: definir `GATEWAY_PROXIES_CONFIABLES` y `UI_PROXIES_CONFIABLES` con `172.19.0.0/16` y reiniciar los dos servicios. Hay que verificar la IP que llega al gateway, porque en Docker Desktop para Mac puede ser la de la VM.
- Contador de intentos de login compartido entre réplicas con Redis, opcional con `GATEWAY_LIMITE_ALMACEN=redis` (#116). Por defecto sigue en memoria. Si Redis cae, el login pasa y el bloqueo de cuenta sigue activo.
- Cobertura del cifrado (#123, #124): `clientes`, `usuarios-accesos` y `compras` cifran sus datos personales y fiscales. Decisiones: la razón social de cliente y el título de documentos quedan en claro. El resumen de acciones de nexus ya no guarda nombre ni correo (#125). Pendiente: el campo `payload` de esa misma acción, que sí guarda nombre y correo porque se necesitan para crear el prospecto. Está anotado en `docs/seguridad/auditoria-cifrado-campos.md`.
- Validación del título de documentos: pendiente de definir el formato aceptado.
- Kubernetes: manifiestos base en `infra/kubernetes/` (#99). Faltan el registro real, el dominio y las variables sensibles pasadas a `Secret`, y la verificación contra un cluster.

## 3. Registro de PRs y merges (al final)

**Se hace al final**, cuando el backend esté completo y todos los PR y merges estén registrados. Así la documentación queda completa de una sola vez.

- [ ] Listar cada PR de la sesión en una tabla con: número, título, rama, servicios afectados, commit de merge, fecha y resumen de una línea.
- [ ] Incluir las decisiones de cada PR (por ejemplo, el bloqueo de 5 minutos o la cuenta por pagar) y enlazar cada fila con su PR en GitHub.
- [ ] Ubicar la tabla en la documentación del repositorio, con enlace desde el README principal.

**Pendiente:** no iniciar hasta que el backend esté terminado.
