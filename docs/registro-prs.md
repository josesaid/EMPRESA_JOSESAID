# Registro de PRs de la sesión de seguridad y pruebas

Lista de los PR fusionados de #109 a #263, con su commit de fusión, fecha y servicios afectados. Los servicios salen de los archivos que cambió cada PR.

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
| [#150](https://github.com/josesaid/EMPRESA_JOSESAID/pull/150) | docs: registro de PRs de la sesión (#109 a #149) | `docs/registro-prs` | Documentación | `34f8c77` | 2026-11-08 |
| [#151](https://github.com/josesaid/EMPRESA_JOSESAID/pull/151) | docs: guías publicadas en HTML y enlazadas desde los READMEs de servicio | `docs/guias-html` | Todos los servicios | `839ec8d` | 2026-11-08 |
| [#152](https://github.com/josesaid/EMPRESA_JOSESAID/pull/152) | build(nexus): JaCoCo mide cobertura y piso de 0.85 | `calidad/nexus-jacoco` | nexus-ms | `e75bda6` | 2026-11-08 |
| [#153](https://github.com/josesaid/EMPRESA_JOSESAID/pull/153) | docs(pendientes): marcar lo completado en calidad y seguridad | `docs/checkboxes-pendientes` | Documentación | `33dc79f` | 2026-11-08 |
| [#154](https://github.com/josesaid/EMPRESA_JOSESAID/pull/154) | docs(ui-ms): agregar README con rutas, configuración y guías | `docs/ui-readme` | ui-ms | `215da40` | 2026-11-08 |
| [#155](https://github.com/josesaid/EMPRESA_JOSESAID/pull/155) | docs(pendientes): marcar como omitidos Kubernetes y validación del título | `docs/omitidos-cierre` | Documentación | `b167ae4` | 2026-11-08 |
| [#156](https://github.com/josesaid/EMPRESA_JOSESAID/pull/156) | docs(nexus): catálogo de endpoints para consumo desde IA | `docs/catalogo-endpoints-ia` | nexus-ms | `61c4d5a` | 2026-11-08 |
| [#157](https://github.com/josesaid/EMPRESA_JOSESAID/pull/157) | feat(nexus): herramienta solicitar_eliminacion_documento | `feat/nexus-eliminacion-documento` | nexus-ms | `6a65059` | 2026-11-09 |
| [#158](https://github.com/josesaid/EMPRESA_JOSESAID/pull/158) | feat(nexus): lecturas de cotizaciones, pedidos y catálogos de ventas | `feat/nexus-lecturas-pedidos` | nexus-ms | `2932dec` | 2026-11-09 |
| [#159](https://github.com/josesaid/EMPRESA_JOSESAID/pull/159) | feat(nexus): herramientas de clientes, contactos y prospectos | `feat/nexus-clientes` | nexus-ms | `4b186ee` | 2026-11-09 |
| [#160](https://github.com/josesaid/EMPRESA_JOSESAID/pull/160) | feat(nexus): herramientas de compras (proveedores, órdenes, recepción, cancelación) | `feat/nexus-compras` | nexus-ms | `fd57d9b` | 2026-11-09 |
| [#161](https://github.com/josesaid/EMPRESA_JOSESAID/pull/161) | feat(nexus): herramientas de inventario (artículos, movimientos, órdenes de operación) | `feat/nexus-inventario` | nexus-ms | `2b6dec5` | 2026-11-09 |
| [#162](https://github.com/josesaid/EMPRESA_JOSESAID/pull/162) | feat(nexus): cobro de comprobantes y consulta de cuentas por pagar | `feat/nexus-facturacion` | nexus-ms | `130628c` | 2026-11-09 |
| [#163](https://github.com/josesaid/EMPRESA_JOSESAID/pull/163) | feat(nexus): consulta del catálogo de tipos de documento | `feat/nexus-documentos-catalogo` | nexus-ms | `7faca7d` | 2026-11-09 |
| [#164](https://github.com/josesaid/EMPRESA_JOSESAID/pull/164) | feat(nexus): consulta del estado de una acción y catálogo actualizado | `feat/nexus-estado-accion` | nexus-ms | `c6f46eb` | 2026-11-09 |
| [#165](https://github.com/josesaid/EMPRESA_JOSESAID/pull/165) | feat(nexus): cuatro escrituras de bajo riesgo como herramientas | `feat/nexus-escrituras-seguras` | nexus-ms | `451c181` | 2026-11-10 |
| [#166](https://github.com/josesaid/EMPRESA_JOSESAID/pull/166) | test(smoke): usar parametros validos en inventario y reportes | `chore/smoke-e2e-parametros-validos` | Documentación | `964afaa` | 2026-11-09 |
| [#167](https://github.com/josesaid/EMPRESA_JOSESAID/pull/167) | fix(clientes): fecha de alta y registro en hora de Mexico | `fix/clientes-fecha-negocio` | clientes-ms | `12bbd4c` | 2026-11-09 |
| [#168](https://github.com/josesaid/EMPRESA_JOSESAID/pull/168) | fix: fechas de negocio en hora de Mexico (contabilidad, pedidos, facturacion, documentos, compras) | `fix/fechas-negocio-utc` | compras-proveedores-ms, contabilidad-ms, documentos-cumplimiento-ms, facturacion-pagos-ms, pedidos-ventas-ms | `3e276cd` | 2026-11-09 |
| [#169](https://github.com/josesaid/EMPRESA_JOSESAID/pull/169) | fix(nexus): no redactar UUID como teléfono en el filtro de datos sensibles | `fix/filtro-telefono-uuid` | nexus-comun | `b739dbb` | 2026-11-09 |
| [#170](https://github.com/josesaid/EMPRESA_JOSESAID/pull/170) | docs: piloto de Nexus por áreas (2026-11-08) | `docs/piloto-nexus-2026-11-08` | Documentación | `aade8d0` | 2026-11-09 |
| [#171](https://github.com/josesaid/EMPRESA_JOSESAID/pull/171) | feat(nexus): prompt asistente-base v2, no suponer significados de números | `feat/nexus-no-suponer-significados` | nexus-comun | `266a7ba` | 2026-11-09 |
| [#172](https://github.com/josesaid/EMPRESA_JOSESAID/pull/172) | test(clientes): cubre Interaccion y RecifradorCamposRunner | `test/clientes-cobertura-interaccion-recifrador` | clientes-ms | `2a387a0` | 2026-11-09 |
| [#173](https://github.com/josesaid/EMPRESA_JOSESAID/pull/173) | docs(nexus-comun): README del módulo compartido de Nexus | `docs/readme-nexus-comun` | nexus-comun | `e42891c` | 2026-11-09 |
| [#174](https://github.com/josesaid/EMPRESA_JOSESAID/pull/174) | docs: diagramas de flujos entre áreas | `docs/diagramas-areas` | pedidos-ventas-ms | `cac1438` | 2026-11-09 |
| [#175](https://github.com/josesaid/EMPRESA_JOSESAID/pull/175) | docs: diagramas de gateway y discovery | `docs/diagramas-gateway-discovery` | discovery-ms, gateway-ms | `23b2f3c` | 2026-11-09 |
| [#176](https://github.com/josesaid/EMPRESA_JOSESAID/pull/176) | docs(pedidos): diagramas del servicio | `docs/diagramas-pedidos` | pedidos-ventas-ms | `1c317bf` | 2026-11-09 |
| [#177](https://github.com/josesaid/EMPRESA_JOSESAID/pull/177) | docs(compras): diagramas del servicio | `docs/diagramas-compras` | compras-proveedores-ms | `5cab580` | 2026-11-09 |
| [#178](https://github.com/josesaid/EMPRESA_JOSESAID/pull/178) | docs(facturacion): diagramas del servicio | `docs/diagramas-facturacion` | facturacion-pagos-ms | `efaa994` | 2026-11-09 |
| [#179](https://github.com/josesaid/EMPRESA_JOSESAID/pull/179) | docs(usuarios): diagramas de estados, duplicados, rotación RS256, área y contraseña temporal | `docs/usuarios-diagramas-estado-duplicados-y-mas` | usuarios-accesos-ms | `c9caafa` | 2026-11-09 |
| [#180](https://github.com/josesaid/EMPRESA_JOSESAID/pull/180) | docs(contabilidad): diagramas de póliza, duplicados e Idempotency-Key | `docs/contabilidad-diagramas-estado-duplicados-idempotencia` | contabilidad-ms | `54748df` | 2026-11-09 |
| [#181](https://github.com/josesaid/EMPRESA_JOSESAID/pull/181) | docs(inventario): diagramas de orden, existencia, duplicados e Idempotency-Key | `docs/inventario-diagramas-estado-duplicados-idempotencia` | inventario-operaciones-ms | `2122cdc` | 2026-11-09 |
| [#182](https://github.com/josesaid/EMPRESA_JOSESAID/pull/182) | docs(reportes): diagramas de reglas de estado, duplicados y lectura por área | `docs/reportes-diagramas-estado-duplicados-lectura-area` | reportes-ejecutivos-ms | `020b2a8` | 2026-11-09 |
| [#183](https://github.com/josesaid/EMPRESA_JOSESAID/pull/183) | docs(documentos): diagramas de reglas de estado, duplicados y retención | `docs/documentos-diagramas-estado-duplicados-retencion` | documentos-cumplimiento-ms | `5ee90bd` | 2026-11-09 |
| [#184](https://github.com/josesaid/EMPRESA_JOSESAID/pull/184) | docs(clientes): diagrama de roles y datos personales enmascarados | `docs/clientes-diagrama-roles-datos-personales` | clientes-ms | `30fb024` | 2026-11-09 |
| [#185](https://github.com/josesaid/EMPRESA_JOSESAID/pull/185) | docs(ui): diagramas de arquitectura, login, renovación con lock y cierre | `docs/ui-diagramas-login-renovacion-cierre` | ui-ms | `fd5ba9c` | 2026-11-09 |
| [#186](https://github.com/josesaid/EMPRESA_JOSESAID/pull/186) | docs(nexus): diagramas de arquitectura, acciones con confirmación y acciones por MCP | `docs/nexus-diagramas-arquitectura-acciones-mcp` | nexus-ms | `df36f65` | 2026-11-09 |
| [#187](https://github.com/josesaid/EMPRESA_JOSESAID/pull/187) | docs(pendientes): marca como completados los diagramas ya fusionados | `docs/backlog-diagramas-completo` | Documentación | `9183da1` | 2026-11-09 |
| [#188](https://github.com/josesaid/EMPRESA_JOSESAID/pull/188) | test(nexus): LineaBaseIT solo corre con NEXUS_LINEA_BASE=1 | `fix/nexus-linea-base-compuerta` | nexus-ms | `798622f` | 2026-11-09 |
| [#189](https://github.com/josesaid/EMPRESA_JOSESAID/pull/189) | fix(nexus-comun): cierra streams, copia colecciones y filtra EI_EXPOSE_REP2 en dos clases | `fix/nexus-comun-spotbugs` | nexus-comun | `a7c1c1e` | 2026-11-09 |
| [#190](https://github.com/josesaid/EMPRESA_JOSESAID/pull/190) | docs(reportes): ajusta la etiqueta de periodo en el diagrama de reglas de estado | `docs/reportes-ajuste-periodo-diagrama` | reportes-ejecutivos-ms | `840c641` | 2026-11-09 |
| [#191](https://github.com/josesaid/EMPRESA_JOSESAID/pull/191) | docs: fuentes Mermaid para los 12 diagramas de arquitectura | `docs/arquitectura-fuentes-mermaid` | Todos los servicios | `a5d83b9` | 2026-11-09 |
| [#192](https://github.com/josesaid/EMPRESA_JOSESAID/pull/192) | docs: actualiza backlog de diagramas tras PR #191 | `docs/backlog-arquitectura-mmd` | Documentación | `50901dd` | 2026-11-09 |
| [#193](https://github.com/josesaid/EMPRESA_JOSESAID/pull/193) | docs: reordena el diagrama de arquitectura general | `docs/arquitectura-general-layout-2` | Documentación | `97290b3` | 2026-11-09 |
| [#194](https://github.com/josesaid/EMPRESA_JOSESAID/pull/194) | feat(ui-ms): panel con barra lateral y pantallas de consulta por módulo | `feat/ui-dashboard-modulos` | ui-ms | `6b0de4b` | 2026-11-09 |
| [#195](https://github.com/josesaid/EMPRESA_JOSESAID/pull/195) | fix(ui-ms): mostrar listados paginados y clientes anidados | `fix/clientes-listado-anidado` | ui-ms | `aae816c` | 2026-11-09 |
| [#196](https://github.com/josesaid/EMPRESA_JOSESAID/pull/196) | fix(nexus-ms): omitir controles de calidad al compilar nexus-comun en Docker | `fix/nexus-comun-build-sin-checks` | nexus-ms | `9170034` | 2026-11-09 |
| [#197](https://github.com/josesaid/EMPRESA_JOSESAID/pull/197) | feat(inventario): reservar al confirmar y descontar al entregar pedidos | `feat/inventario-consume-pedidos` | inventario-operaciones-ms, pedidos-ventas-ms | `61f7927` | 2026-11-09 |
| [#198](https://github.com/josesaid/EMPRESA_JOSESAID/pull/198) | feat(inventario): sumar existencias con las recepciones de compras | `feat/inventario-consume-recepciones` | compras-proveedores-ms, inventario-operaciones-ms | `c393b95` | 2026-11-09 |
| [#199](https://github.com/josesaid/EMPRESA_JOSESAID/pull/199) | fix(ui): ocultar columnas sin valor en los listados | `fix/clientes-prospecto-id` | ui-ms | `4d54535` | 2026-11-09 |
| [#200](https://github.com/josesaid/EMPRESA_JOSESAID/pull/200) | feat(ui): color de la barra lateral según el rol del usuario | `feat/ui-color-por-rol` | ui-ms | `34ab704` | 2026-11-09 |
| [#201](https://github.com/josesaid/EMPRESA_JOSESAID/pull/201) | feat(ui): formularios de alta de clientes y proveedores | `feat/ui-altas-clientes-proveedores` | ui-ms | `9cde83a` | 2026-11-09 |
| [#202](https://github.com/josesaid/EMPRESA_JOSESAID/pull/202) | feat(ui): panel de sugerencias de IA en la esquina inferior derecha | `feat/ui-panel-sugerencias-ia` | ui-ms | `81ef76d` | 2026-11-09 |
| [#203](https://github.com/josesaid/EMPRESA_JOSESAID/pull/203) | feat(ui): pasos para llevar a cabo cada sugerencia de IA y panel más ancho | `feat/ui-panel-sugerencias-pasos` | ui-ms | `2a18d58` | 2026-11-09 |
| [#204](https://github.com/josesaid/EMPRESA_JOSESAID/pull/204) | feat(ui): panel de sugerencias con tamaño automático y pasos concretos | `feat/ui-panel-pasos-concretos` | ui-ms | `d63ee1d` | 2026-11-09 |
| [#205](https://github.com/josesaid/EMPRESA_JOSESAID/pull/205) | fix(ui): pasos de contador usan solo controles existentes | `feat/sugerencias-pasos-existentes` | ui-ms | `09ab450` | 2026-11-09 |
| [#206](https://github.com/josesaid/EMPRESA_JOSESAID/pull/206) | fix(ui): sugerencias usan solo pantallas, filtros y botones existentes | `feat/sugerencias-pasos-verificados` | ui-ms | `059b828` | 2026-11-09 |
| [#207](https://github.com/josesaid/EMPRESA_JOSESAID/pull/207) | feat(ui): opción Agiliza con IA y panel de sugerencias colapsable | `feat/agiliza-ia` | ui-ms | `c8f4daa` | 2026-11-09 |
| [#208](https://github.com/josesaid/EMPRESA_JOSESAID/pull/208) | feat(ui): Agiliza con IA muestra el dato a capturar y lleva a la pantalla | `feat/agiliza-tareas-dinamicas` | ui-ms | `a962a07` | 2026-11-09 |
| [#209](https://github.com/josesaid/EMPRESA_JOSESAID/pull/209) | feat(ui): Agiliza con IA con radiobotones debajo del texto y sin pasos | `feat/agiliza-radios` | ui-ms | `955a35d` | 2026-11-09 |
| [#210](https://github.com/josesaid/EMPRESA_JOSESAID/pull/210) | fix(ui): el correo del usuario logueado aparece en el encabezado en todas las pantallas | `fix/correo-en-header` | ui-ms | `a8e5c8a` | 2026-11-09 |
| [#211](https://github.com/josesaid/EMPRESA_JOSESAID/pull/211) | feat(ui): panel de sugerencias 20% más ancho y 40% menos alto; login rediseñado | `feat/panel-y-login-visual` | ui-ms | `f69ff38` | 2026-11-09 |
| [#212](https://github.com/josesaid/EMPRESA_JOSESAID/pull/212) | feat(ui): pantalla Asistente Nexus (chat con /nexus/consultas) bajo Inicio | `feat/asistente-nexus` | ui-ms | `883dfa4` | 2026-11-09 |
| [#213](https://github.com/josesaid/EMPRESA_JOSESAID/pull/213) | fix(ui): Mensaje del asistente Nexus serializable para la sesión en Redis | `fix/asistente-rutas` | ui-ms | `cfa654f` | 2026-11-09 |
| [#214](https://github.com/josesaid/EMPRESA_JOSESAID/pull/214) | feat(nexus): tope diario de tokens configurable desde el .env de la raíz | `feat/nexus-presupuesto-env` | nexus-ms | `7d436c0` | 2026-11-09 |
| [#215](https://github.com/josesaid/EMPRESA_JOSESAID/pull/215) | fix(clientes): permitir rol ADMIN en endpoints de ventas y legal | `fix/admin-acceso-clientes` | clientes-ms | `8cb35a0` | 2026-11-10 |
| [#216](https://github.com/josesaid/EMPRESA_JOSESAID/pull/216) | feat(nexus): limitar el asistente a la operación de la empresa | `feat/nexus-guardrail-alcance` | nexus-comun | `30d5604` | 2026-11-10 |
| [#217](https://github.com/josesaid/EMPRESA_JOSESAID/pull/217) | fix(seguridad): ADMIN hereda todos los roles de negocio | `fix/admin-jerarquia-roles` | clientes-ms, compras-proveedores-ms, contabilidad-ms, documentos-cumplimiento-ms, facturacion-pagos-ms, inventario-operaciones-ms, pedidos-ventas-ms, reportes-ejecutivos-ms | `280b05e` | 2026-11-10 |
| [#218](https://github.com/josesaid/EMPRESA_JOSESAID/pull/218) | feat(scripts): agregar stop.sh para apagar el entorno | `feature/stop-sh-script-file` | Documentación | `5b2d769` | 2026-11-10 |
| [#219](https://github.com/josesaid/EMPRESA_JOSESAID/pull/219) | feat(scripts): agregar start.sh para levantar el entorno | `feature/start-sh-script-file` | Documentación | `187b606` | 2026-11-10 |
| [#220](https://github.com/josesaid/EMPRESA_JOSESAID/pull/220) | refactor(scripts): aplicar el mismo estilo visual a stop.sh | `feature/stop-sh-estilo` | Documentación | `12e9422` | 2026-11-10 |
| [#221](https://github.com/josesaid/EMPRESA_JOSESAID/pull/221) | feat(scripts): start.sh abre Docker Desktop y espera a que esté listo | `feature/start-sh-docker-auto` | Documentación | `80d8764` | 2026-11-10 |
| [#222](https://github.com/josesaid/EMPRESA_JOSESAID/pull/222) | fix(scripts): stop.sh informa cuando Docker ya está apagado | `feature/stop-sh-docker-apagado` | Documentación | `59818ed` | 2026-11-10 |
| [#223](https://github.com/josesaid/EMPRESA_JOSESAID/pull/223) | feat(scripts): agregar rm-target-directories.sh para borrar targets de Maven | `feature/rm-target-directories` | Documentación | `c72c498` | 2026-11-10 |
| [#224](https://github.com/josesaid/EMPRESA_JOSESAID/pull/224) | feat(infra): healthchecks en todos los servicios y mejoras de build/start | `feature/configuration-scripts` | Documentación | `fdae7c9` | 2026-11-10 |
| [#225](https://github.com/josesaid/EMPRESA_JOSESAID/pull/225) | feat(docs): documentación Swagger/OpenAPI de cada microservicio | `feature/swagger-docs` | Documentación | `21ab03f` | 2026-11-10 |
| [#226](https://github.com/josesaid/EMPRESA_JOSESAID/pull/226) | chore(scripts): stop.sh con flags y build.sh con candado | `chore/scripts-stop-build` | Documentación | `752edc5` | 2026-11-10 |
| [#227](https://github.com/josesaid/EMPRESA_JOSESAID/pull/227) | fix(stop): apagar también el proyecto ui-ms | `fix/stop-ui-ms` | Documentación | `b9a563f` | 2026-11-10 |
| [#228](https://github.com/josesaid/EMPRESA_JOSESAID/pull/228) | fix(start): usar imágenes locales con --pull never | `fix/start-pull-never` | Documentación | `8d1495f` | 2026-11-10 |
| [#229](https://github.com/josesaid/EMPRESA_JOSESAID/pull/229) | ui: iconos en el menú lateral de ui-ms | `feature/menu-iconos-ui` | ui-ms | `e75b440` | 2026-11-10 |
| [#230](https://github.com/josesaid/EMPRESA_JOSESAID/pull/230) | docs: fichas por microservicio con README, permisos, diagramas y Swagger | `feature/fichas-microservicios` | Documentación | `d71cf3c` | 2026-11-10 |
| [#231](https://github.com/josesaid/EMPRESA_JOSESAID/pull/231) | build: javadoc y cobertura en los microservicios, y páginas de tecnologías | `feature/javadoc-cobertura-plugins` | clientes-ms, compras-proveedores-ms, contabilidad-ms, discovery-ms | `fd23642` | 2026-11-10 |
| [#232](https://github.com/josesaid/EMPRESA_JOSESAID/pull/232) | docs: resultados reales de pruebas y calidad en las fichas técnicas | `feature/resultados-tecnologias` | Documentación | `86a9789` | 2026-11-10 |
| [#233](https://github.com/josesaid/EMPRESA_JOSESAID/pull/233) | build: spotbugs 4.10.5.0 y pmd 3.28.0 | `fix/spotbugs-pmd-versiones` | Todos los servicios | `6d4b2b1` | 2026-11-10 |
| [#234](https://github.com/josesaid/EMPRESA_JOSESAID/pull/234) | nexus: trabajo local en herramientas y temas, y documentación de scripts | `feature/trabajo-local-nexus` | nexus-ms, ui-ms | `d02734f` | 2026-11-10 |
| [#235](https://github.com/josesaid/EMPRESA_JOSESAID/pull/235) | docs: fichas con resultados de calidad, navegación y ajustes de textos | `feature/docs-calidad-y-fichas` | Documentación | `d39db99` | 2026-11-10 |
| [#236](https://github.com/josesaid/EMPRESA_JOSESAID/pull/236) | fix(pruebas): variables de Eureka en validación de clientes-ms | `fix/pruebas-clientes-eureka` | Documentación | `54f0597` | 2026-11-10 |
| [#237](https://github.com/josesaid/EMPRESA_JOSESAID/pull/237) | test(inventario): cubrir listeners y revocaciones para cobertura 0.80 | `feature/tests-inventario-cobertura` | inventario-operaciones-ms | `0686797` | 2026-11-11 |
| [#238](https://github.com/josesaid/EMPRESA_JOSESAID/pull/238) | docs(api): regenerar fichas con resultados reales de pruebas | `feature/fichas-api-regeneradas` | Documentación | `307c62c` | 2026-11-11 |
| [#239](https://github.com/josesaid/EMPRESA_JOSESAID/pull/239) | docs: javadoc en todos los microservicios y nexus-comun | `feature/javadoc-todo` | clientes-ms, compras-proveedores-ms | `3b62f32` | 2026-11-11 |
| [#240](https://github.com/josesaid/EMPRESA_JOSESAID/pull/240) | docs: regenera fichas API con colores y componentes nuevos | `feature/fichas-api-colores` | Documentación | `ee133e0` | 2026-11-11 |
| [#241](https://github.com/josesaid/EMPRESA_JOSESAID/pull/241) | chore: sube cambios pendientes del Mac (start.sh, guías, index, salidas de pruebas) | `feature/sync-pendientes-mac` | Documentación | `42629c6` | 2026-11-11 |
| [#242](https://github.com/josesaid/EMPRESA_JOSESAID/pull/242) | build: genera reportes de calidad, pruebas y Javadoc; visor de código en las fichas | `feature/build-javadoc` | clientes-ms | `2f6a20b` | 2026-11-11 |
| [#243](https://github.com/josesaid/EMPRESA_JOSESAID/pull/243) | docs: @param para los componentes de los records documentados | `feature/params-records` | Todos los servicios | `0034714` | 2026-11-11 |
| [#244](https://github.com/josesaid/EMPRESA_JOSESAID/pull/244) | build: quita Cobertura del proyecto y de las fichas | `feature/quitar-cobertura` | clientes-ms, compras-proveedores-ms, contabilidad-ms, discovery-ms | `73ee8f3` | 2026-11-11 |
| [#245](https://github.com/josesaid/EMPRESA_JOSESAID/pull/245) | docs: estilo para la página de guías; se quita Pendientes de diseño | `feature/guias-estilo` | Documentación | `dd4c6ab` | 2026-11-11 |
| [#246](https://github.com/josesaid/EMPRESA_JOSESAID/pull/246) | build: contador [n/total] en reportes de calidad (paso 6) | `feature/build-contador-reportes` | Documentación | `09e2a95` | 2026-11-11 |
| [#247](https://github.com/josesaid/EMPRESA_JOSESAID/pull/247) | docs: regenerar docs/api tras los cambios de @param y reportes | `docs/regenerar-api` | Documentación | `a6622df` | 2026-11-11 |
| [#248](https://github.com/josesaid/EMPRESA_JOSESAID/pull/248) | docs(manual): mejorar el tema visual del manual | `docs/manual-tema` | Documentación | `e3ac178` | 2026-11-11 |
| [#249](https://github.com/josesaid/EMPRESA_JOSESAID/pull/249) | docs: afinar @param y resumen de reportes en la portada | `docs/params-y-resumen` | clientes-ms, compras-proveedores-ms, contabilidad-ms | `8166358` | 2026-11-11 |
| [#250](https://github.com/josesaid/EMPRESA_JOSESAID/pull/250) | docs(manual): cabecera en tonos dorados | `docs/manual-dorado` | Documentación | `6db59cf` | 2026-11-11 |
| [#251](https://github.com/josesaid/EMPRESA_JOSESAID/pull/251) | docs(guias): cabecera del índice en tonos vino | `docs/guias-readme-lima` | Documentación | `a5ce075` | 2026-11-11 |
| [#252](https://github.com/josesaid/EMPRESA_JOSESAID/pull/252) | docs: páginas HTML para Seguridad y Registro de PRs | `docs/seguridad-html` | Documentación | `8e9ddec` | 2026-11-11 |
| [#253](https://github.com/josesaid/EMPRESA_JOSESAID/pull/253) | docs(registro-prs): cabecera en negro y amarillo | `docs/registro-amarillo` | Documentación | `826c2c7` | 2026-11-11 |
| [#254](https://github.com/josesaid/EMPRESA_JOSESAID/pull/254) | docs(seguridad): mejorar el formato de la tabla de inventario de claves | `docs/seguridad-tablas` | Documentación | `13c983c` | 2026-11-11 |
| [#255](https://github.com/josesaid/EMPRESA_JOSESAID/pull/255) | docs(registro-prs): mejorar el formato de la tabla de PRs | `docs/registro-tablas` | Documentación | `826c2c7` | 2026-11-11 |
| [#256](https://github.com/josesaid/EMPRESA_JOSESAID/pull/256) | docs(seguridad): tabla de inventario al 95% del ancho | `docs/inventario-ancho` | Documentación | `b839bae` | 2026-11-11 |
| [#257](https://github.com/josesaid/EMPRESA_JOSESAID/pull/257) | docs(guias): páginas HTML de cada guía en tono fucsia | `docs/guias-fucsia` | Documentación | `7a67135` | 2026-11-11 |
| [#258](https://github.com/josesaid/EMPRESA_JOSESAID/pull/258) | docs: tabla de reportes de calidad con formato profesional | `docs/tabla-reportes-pro` | Documentación | `6ad1bb5` | 2026-11-11 |
| [#259](https://github.com/josesaid/EMPRESA_JOSESAID/pull/259) | docs(seguridad): tablas con scroll en las páginas restantes | `docs/seguridad-tablas-2` | Documentación | `a3e868e` | 2026-11-11 |
| [#260](https://github.com/josesaid/EMPRESA_JOSESAID/pull/260) | docs(registro-prs): listar todos los PRs con paginación de 20 en 20 | `docs/registro-todos` | Documentación | `4144be5` | 2026-11-11 |
| [#261](https://github.com/josesaid/EMPRESA_JOSESAID/pull/261) | docs(registro-prs): ancho del 95 % en la página de PRs | `docs/registro-ancho` | Documentación | `6e43994` | 2026-11-11 |
| [#262](https://github.com/josesaid/EMPRESA_JOSESAID/pull/262) | docs(seguridad): inventario de claves al 95 % del ancho de la página | `docs/inventario-95` | Documentación | `72f4ff8` | 2026-11-11 |
| [#263](https://github.com/josesaid/EMPRESA_JOSESAID/pull/263) | docs(registro-prs): tabla al 95 % de la pantalla | `docs/registro-ancho-2` | Documentación | `d39c9eb` | 2026-11-11 |
| [#264](https://github.com/josesaid/EMPRESA_JOSESAID/pull/264) | docs: sincronizar el registro de PRs con el último merge | `docs/sincronizar-todo` | Documentación | `1fef4a4` | 2026-11-11 |
| [#265](https://github.com/josesaid/EMPRESA_JOSESAID/pull/265) | docs(guia-pruebas): la cobertura falla el build por servicio | `docs/guia-pruebas-cobertura` | Documentación | `3ee9ce5` | 2026-11-11 |
| [#266](https://github.com/josesaid/EMPRESA_JOSESAID/pull/266) | docs(registro): registro de PRs hasta #263 en el archivo Markdown | `docs/registro-prs-md` | Documentación | `d60a50b` | 2026-11-11 |

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
