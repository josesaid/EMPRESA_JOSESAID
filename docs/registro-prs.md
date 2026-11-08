# Registro de PRs de la sesión de seguridad y pruebas

Lista de los PR fusionados de #109 a #149, con su commit de fusión, fecha y servicios afectados. Los servicios salen de los archivos que cambió cada PR.

Enlace desde el [README principal](../README.md).

| PR | Título | Rama | Servicios | Commit de fusión | Fecha |
|---|---|---|---|---|---|
| [#109](https://github.com/josesaid/EMPRESA_JOSESAID/pull/109) | Documentar avances de seguridad del 2026-11-05 en los pendientes del backend | `seguridad/actualizar-pendientes` | pendientes | `b3ebe0f` | 2026-11-06 |
| [#110](https://github.com/josesaid/EMPRESA_JOSESAID/pull/110) | Fijar el digest de la imagen base de runtime en los Dockerfiles | `seguridad/digest-imagen-jre` | Todos los servicios | `9c46a00` | 2026-11-06 |
| [#111](https://github.com/josesaid/EMPRESA_JOSESAID/pull/111) | Fijar el digest de la imagen base de build en los Dockerfiles | `seguridad/digest-imagen-maven` | Todos los servicios | `b0ce870` | 2026-11-06 |
| [#112](https://github.com/josesaid/EMPRESA_JOSESAID/pull/112) | No registrar montos agregados en los logs de indicadores | `seguridad/logs-sin-montos` | clientes-ms, compras-proveedores-ms, contabilidad-ms, documentos-cumplimiento-ms, facturacion-pagos-ms, inventario-operaciones-ms, pedidos-ventas-ms, usuarios-accesos-ms | `b9aa691` | 2026-11-06 |
| [#113](https://github.com/josesaid/EMPRESA_JOSESAID/pull/113) | No registrar mensajes de excepción en los logs de advertencia | `seguridad/logs-excepciones` | Todos los servicios | `74acd30` | 2026-11-06 |
| [#114](https://github.com/josesaid/EMPRESA_JOSESAID/pull/114) | Usar la IP del cliente para el límite de intentos cuando el proxy es de confianza | `seguridad/gateway-ip-cliente` | gateway-ms | `7e708f6` | 2026-11-06 |
| [#115](https://github.com/josesaid/EMPRESA_JOSESAID/pull/115) | Enviar la IP del navegador al gateway en el login de la UI | `seguridad/ui-envia-ip` | ui-ms | `0038a72` | 2026-11-06 |
| [#116](https://github.com/josesaid/EMPRESA_JOSESAID/pull/116) | Compartir el límite de intentos del gateway entre réplicas con Redis | `seguridad/limite-redis` | gateway-ms | `24a0837` | 2026-11-06 |
| [#117](https://github.com/josesaid/EMPRESA_JOSESAID/pull/117) | Registrar resultados de mvn verify en los pendientes del backend | `seguridad/resultados-verify` | pendientes | `0400b76` | 2026-11-06 |
| [#118](https://github.com/josesaid/EMPRESA_JOSESAID/pull/118) | Responder 400 cuando un parámetro de búsqueda excede su longitud | `seguridad/clientes-pruebas-400` | clientes-ms | `5879296` | 2026-11-06 |
| [#119](https://github.com/josesaid/EMPRESA_JOSESAID/pull/119) | Responder 400 cuando un parámetro de búsqueda excede su longitud en cinco servicios | `seguridad/400-parametros-servicios` | compras-proveedores-ms, contabilidad-ms, documentos-cumplimiento-ms, facturacion-pagos-ms, reportes-ejecutivos-ms | `43afbc4` | 2026-11-06 |
| [#120](https://github.com/josesaid/EMPRESA_JOSESAID/pull/120) | Marcar como hecha la sustitución de las variables de clave única | `seguridad/inventario-claves-estado` | docs | `1da88eb` | 2026-11-06 |
| [#121](https://github.com/josesaid/EMPRESA_JOSESAID/pull/121) | Probar el 400 del parámetro razonSocial en compras | `seguridad/compras-pruebas-400` | compras-proveedores-ms | `135eb15` | 2026-11-06 |
| [#122](https://github.com/josesaid/EMPRESA_JOSESAID/pull/122) | Probar el 400 de los parámetros de búsqueda en cuatro servicios | `seguridad/400-pruebas-restantes` | contabilidad-ms, documentos-cumplimiento-ms, facturacion-pagos-ms, reportes-ejecutivos-ms | `68062fc` | 2026-11-06 |
| [#123](https://github.com/josesaid/EMPRESA_JOSESAID/pull/123) | Documentar la auditoría de cobertura del cifrado de campos | `seguridad/auditoria-cifrado` | docs | `df96f42` | 2026-11-06 |
| [#124](https://github.com/josesaid/EMPRESA_JOSESAID/pull/124) | Corregir la fecha de la auditoría de cifrado | `seguridad/auditoria-cifrado-fecha` | docs | `a60ca31` | 2026-11-06 |
| [#125](https://github.com/josesaid/EMPRESA_JOSESAID/pull/125) | No guardar nombre ni correo del prospecto en el resumen de la acción | `seguridad/nexus-resumen-sin-datos` | nexus-ms | `6f49128` | 2026-11-06 |
| [#126](https://github.com/josesaid/EMPRESA_JOSESAID/pull/126) | Actualizar pendientes y la auditoría de cifrado | `docs/pendientes-avances-2026-11-05` | docs, pendientes | `4e0b39e` | 2026-11-06 |
| [#127](https://github.com/josesaid/EMPRESA_JOSESAID/pull/127) | Borrar acciones pendientes vencidas de nexus | `seguridad/nexus-payload-cifrado` | nexus-ms | `2c8b21b` | 2026-11-06 |
| [#128](https://github.com/josesaid/EMPRESA_JOSESAID/pull/128) | Actualizar la auditoría de cifrado con la limpieza del payload de nexus | `docs/auditoria-payload-nexus` | docs | `8f74012` | 2026-11-06 |
| [#129](https://github.com/josesaid/EMPRESA_JOSESAID/pull/129) | Registrar la IP de los intentos de login a nivel DEBUG | `seguridad/log-debug-ip-login` | gateway-ms | `1bddfd1` | 2026-11-06 |
| [#130](https://github.com/josesaid/EMPRESA_JOSESAID/pull/130) | Agregar script para verificar el login de la UI | `docs/script-verificar-login` | scripts | `42c8eac` | 2026-11-06 |
| [#131](https://github.com/josesaid/EMPRESA_JOSESAID/pull/131) | Fijar la versión de la imagen del gateway en Kubernetes | `seguridad/k8s-gateway-tag` | infra | `de4d90c` | 2026-11-06 |
| [#132](https://github.com/josesaid/EMPRESA_JOSESAID/pull/132) | Agregar prueba de humo del stack | `docs/script-smoke-e2e` | scripts | `4f644a6` | 2026-11-06 |
| [#133](https://github.com/josesaid/EMPRESA_JOSESAID/pull/133) | Corregir el arranque de nexus: LimpiezaAcciones no inyecta Clock | `seguridad/fix-limpieza-bean` | nexus-ms | `fa5daaf` | 2026-11-06 |
| [#134](https://github.com/josesaid/EMPRESA_JOSESAID/pull/134) | Pruebas de autorización por rol en clientes (piloto) | `test/autorizacion-clientes` | clientes-ms | `d81ed01` | 2026-11-06 |
| [#135](https://github.com/josesaid/EMPRESA_JOSESAID/pull/135) | Pruebas de autorización por rol en compras | `test/autorizacion-compras` | compras-proveedores-ms | `b019f65` | 2026-11-06 |
| [#136](https://github.com/josesaid/EMPRESA_JOSESAID/pull/136) | Pruebas de autorización por rol en contabilidad | `test/autorizacion-contabilidad` | contabilidad-ms | `b74b449` | 2026-11-06 |
| [#137](https://github.com/josesaid/EMPRESA_JOSESAID/pull/137) | Pruebas de autorización por rol en documentos | `test/autorizacion-documentos` | documentos-cumplimiento-ms | `cfc9655` | 2026-11-06 |
| [#138](https://github.com/josesaid/EMPRESA_JOSESAID/pull/138) | test(facturacion): matriz de autorización por rol | `test/autorizacion-facturacion` | facturacion-pagos-ms | `01118c4` | 2026-11-06 |
| [#139](https://github.com/josesaid/EMPRESA_JOSESAID/pull/139) | test(inventario): matriz de autorización por rol | `test/autorizacion-inventario` | inventario-operaciones-ms | `a680b7d` | 2026-11-06 |
| [#140](https://github.com/josesaid/EMPRESA_JOSESAID/pull/140) | test(pedidos): matriz de autorización por rol | `test/autorizacion-pedidos` | pedidos-ventas-ms | `1685d76` | 2026-11-06 |
| [#141](https://github.com/josesaid/EMPRESA_JOSESAID/pull/141) | test(reportes): matriz de autorización por rol | `test/autorizacion-reportes` | reportes-ejecutivos-ms | `d13ec16` | 2026-11-06 |
| [#142](https://github.com/josesaid/EMPRESA_JOSESAID/pull/142) | test(nexus): matriz de autorización por rol | `test/autorizacion-nexus` | nexus-ms | `ceedf98` | 2026-11-06 |
| [#143](https://github.com/josesaid/EMPRESA_JOSESAID/pull/143) | test(usuarios-accesos): matriz de autorización de /v1/usuarios | `test/autorizacion-usuarios` | usuarios-accesos-ms | `6fdc504` | 2026-11-06 |
| [#144](https://github.com/josesaid/EMPRESA_JOSESAID/pull/144) | docs(pendientes): registrar decisiones de TLS y límite de cuerpo | `docs/decisiones-tls-limite-cuerpo` | pendientes | `9156d83` | 2026-11-06 |
| [#145](https://github.com/josesaid/EMPRESA_JOSESAID/pull/145) | docs(pendientes): resultado de pruebas de integración con Docker | `docs/resultado-failsafe` | pendientes | `1bcd1b1` | 2026-11-08 |
| [#146](https://github.com/josesaid/EMPRESA_JOSESAID/pull/146) | fix(seguridad): versiones corregidas de dependencias transitivas | `seguridad/dependencias-versiones` | Todos los servicios | `75a4c12` | 2026-11-08 |
| [#147](https://github.com/josesaid/EMPRESA_JOSESAID/pull/147) | fix(seguridad): actualizar OpenTelemetry a 1.67.0 | `seguridad/opentelemetry` | Todos los servicios | `aeeddd0` | 2026-11-08 |
| [#148](https://github.com/josesaid/EMPRESA_JOSESAID/pull/148) | fix(seguridad): actualizar apt en la imagen de runtime | `seguridad/base-apt` | Todos los servicios | `bf15aea` | 2026-11-08 |
| [#149](https://github.com/josesaid/EMPRESA_JOSESAID/pull/149) | build(calidad): piso de cobertura por servicio y fallo del build | `calidad/umbrales-cobertura` | Todos los servicios | `4d4ae13` | 2026-11-08 |

## Decisiones registradas

- **#64 bloqueo de cuenta a 5 minutos:** decisión de Said Olano.
- **#83 y #84 TLS interno:** desactivado por defecto; se activa con CA interna (opt-in).
- **#103 límite de cuerpo en el gateway:** 64 KiB.
- **#114 IP del cliente:** solo se usa cuando el proxy es de confianza.
- **#116 Redis para el límite de intentos:** opcional con `GATEWAY_LIMITE_ALMACEN=redis`; por defecto usa memoria.
- **#123 y #124 auditoría de cifrado:** razón social y título se dejan en claro por decisión.
- **#127 limpieza del payload de nexus:** se borran las acciones pendientes vencidas.
- **#146 a #148 dependencias, OpenTelemetry y apt:** se fijan versiones corregidas; Spring Boot se mantiene en 4.1.2.
- **#149 pisos de cobertura:** cada servicio falla si baja de su cobertura actual con integración. nexus-ms queda pendiente porque no genera datos de JaCoCo.
- **Pendientes sin decisión:** ARCO, rotación de la clave HMAC de compras y clave NVD quedan omitidos por decisión.
