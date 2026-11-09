# nexus-comun

## 1. Identidad

- **Nombre:** nexus-comun
- **Tipo:** librería Java compartida. No es un servicio: no tiene contenedor, puerto ni base de datos.
- **Consumidores:** `nexus-ms` (dependencia Maven local, versión 0.1.0)
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · Spring AI

## 2. Propósito y alcance

Reúne las piezas de Nexus que no dependen de un servicio concreto y que deben comportarse igual en producción y en pruebas:

- **Filtro de datos sensibles:** redacta RFC, correos, teléfonos, CLABE y tarjetas antes de que un texto salga hacia el modelo.
- **Registro de tokens:** mide tokens y costo estimado de cada llamada al modelo y los publica como métricas de Micrometer.
- **Prompts versionados:** carga cada prompt desde `resources` en su versión vigente, y conserva las anteriores para revertir.
- **Evaluaciones:** ejecuta casos de calidad con reglas deterministas sobre las respuestas del asistente.

No incluye: llamadas al modelo, herramientas de negocio ni acceso a datos. Eso vive en `nexus-ms`.

## 3. Estructura del repositorio

```
nexus-comun/
├── pom.xml                                   Dependencias (spring-ai-client-chat, micrometer-core)
└── src/
    ├── main/java/.../nexus/comun/
    │   ├── NexusComunVersion.java            Marca de versión del módulo
    │   ├── filtro/                           FiltroDatosSensibles y su advisor
    │   ├── tokens/                           RegistroTokens, PreciosTokens y su advisor
    │   ├── prompts/                          RepositorioPrompts y PromptVersionado
    │   └── evaluacion/                       EjecutorEvaluaciones, CargadorCasos, CasoEvaluacion, ResultadoCaso
    ├── main/resources/nexus/
    │   ├── prompts/indice.properties         Versión vigente de cada prompt
    │   ├── prompts/asistente-base/v1.txt     Prompt original
    │   ├── prompts/asistente-base/v2.txt     Prompt vigente (agrega la regla de no suponer significados)
    │   └── evaluaciones/casos.tsv            Casos de evaluación del piloto
    └── test/java/.../nexus/comun/
        ├── filtro/                           FiltroDatosSensiblesTest, FiltroDatosSensiblesAdvisorTest
        ├── tokens/                           RegistroTokensTest
        ├── prompts/                          RepositorioPromptsTest
        └── evaluacion/                       EjecutorEvaluacionesTest
```

## 4. Requisitos

- JDK 21 y Maven 3.9+.
- No requiere Docker ni base de datos.

## 5. Ejecución

Esta librería se compila dentro de la imagen de `nexus-ms` (ver `nexus-ms/Dockerfile`), que la instala en el repositorio Maven antes de compilar el servicio. Para trabajar en ella sola:

```bash
cd nexus-comun
mvn -B verify
```

Para instalarla en tu repositorio local (lo que usa `nexus-ms` al compilar fuera de Docker):

```bash
mvn -B -q install -DskipTests
```

## 6. Componentes

### 6.1 Filtro de datos sensibles

`FiltroDatosSensibles.redactar(texto)` reemplaza cada dato sensible por una marca:

| Tipo | Marca | Patrón |
|---|---|---|
| RFC (persona física y moral) | `[RFC]` | 3-4 letras, fecha AAMMDD y homoclave |
| Correo | `[CORREO]` | `usuario@dominio.ext` |
| Teléfono MX (10 dígitos) | `[TELEFONO]` | Con o sin prefijo 52 y separadores |
| CLABE (18 dígitos) | `[CUENTA]` | 18 dígitos aislados |
| Tarjeta (16 dígitos) | `[TARJETA]` | Cuatro grupos de cuatro |

Reglas que evitan falsos positivos: montos con coma o punto decimal, folios, años y UUID no se redactan. El teléfono no puede estar pegado a letras, dígitos ni guiones; por eso `6a78ec71-9742-4122-...` no se corta (corrección de PR #169).

`FiltroDatosSensiblesAdvisor` aplica el filtro a los mensajes de usuario antes de llamar al modelo. Corre primero en la cadena de advisors. Acepta una clave en el contexto (`correoPermitido`) para no redactar correos cuando un administrador pide un alta.

### 6.2 Registro de tokens

`RegistroTokensAdvisor` lee el uso de tokens de cada respuesta y llama a `RegistroTokens`, que publica:

- `nexus.tokens` (contador) con etiquetas `servicio`, `usuario` y `direccion` (`entrada` o `salida`).
- `nexus.costo.estimado` (contador) con etiquetas `servicio` y `usuario`.

El costo sale de `PreciosTokens`, que recibe el precio por millón de tokens desde configuración. Si no hay precio, el costo queda en cero. Si la petición no trae `servicio` o `usuario`, usa `desconocido`.

### 6.3 Prompts versionados

`RepositorioPrompts.obtener(nombre)` devuelve la versión vigente indicada en `indice.properties`. `obtener(nombre, version)` devuelve una versión concreta, útil para revertir.

Para publicar un cambio de prompt:
1. Crear el archivo nuevo (`asistente-base/v3.txt`, por ejemplo). No modificar los anteriores.
2. Subir la versión en `indice.properties`.
3. Actualizar `RepositorioPromptsTest` con la versión y el texto esperados.

Versión actual de `asistente-base`: **2**.

### 6.4 Evaluaciones

`CargadorCasos` lee `evaluaciones/casos.tsv` (columnas separadas por tabulador: `id`, `pregunta`, `debe_contener`, `no_debe_contener`, con listas separadas por `|`). `EjecutorEvaluaciones` pasa cada pregunta por un generador y aprueba la respuesta si:

- contiene todas las palabras de `debe_contener`,
- no contiene ninguna de `no_debe_contener`,
- no contiene datos sensibles (redactados con el mismo filtro).

El resultado es un `Informe` con total, aprobados y el detalle por caso. Las reglas son deterministas: no llaman al modelo; el modelo lo conecta `nexus-ms` en sus pruebas de línea base.

## 7. Pruebas

| Clase | Qué cubre |
|---|---|
| `FiltroDatosSensiblesTest` | Cada tipo de dato, falsos positivos (montos, folios, UUID) y conteos |
| `FiltroDatosSensiblesAdvisorTest` | Redacción de correo por defecto y paso del correo cuando el contexto lo permite |
| `RegistroTokensTest` | Suma por servicio y usuario, costo por millón, sin precio no registra costo, `desconocido` si faltan datos, rechazo de precios negativos |
| `RepositorioPromptsTest` | Carga de la versión vigente, versión inexistente y prompt desconocido |
| `EjecutorEvaluacionesTest` | Aprobación, faltantes y prohibidos, datos sensibles en la respuesta y línea base sin generador real |

Resultado de referencia: **36 pruebas, 0 fallos** (`mvn -B verify`).

En tu Mac también existe `ejecutar-pruebas-nexus.sh` en la raíz del repositorio: corre estas pruebas y deja el log en `.verify-nexus.log`.

## 8. Decisiones y pendientes

- **RFC sin redactar hacia el modelo (decidido).** `FiltroDatosSensiblesAdvisor` omite siempre el tipo RFC, así que los RFC llegan al modelo. Es intencional: las herramientas de búsqueda necesitan el RFC para localizar al proveedor o cliente.
- **Evaluaciones con filtro completo.** La regla de datos sensibles de `EjecutorEvaluaciones` usa todos los tipos, incluido RFC, mientras que el advisor no redacta RFC. Pendiente unificar criterio.
- **Costo estimado.** Depende de que `nexus-ms` configure los precios; sin configuración el costo es cero.
- **Casos de evaluación.** `casos.tsv` cubre el piloto. Ampliarlo cuando se agreguen áreas nuevas.

## 9. Referencias

- `nexus-ms/README.md`: servicio que consume esta librería.
- `PILOTO_NEXUS_2026-11-08.md`: resultados del piloto por área.
