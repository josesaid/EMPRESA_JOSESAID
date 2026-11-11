#!/usr/bin/env bash
# Compila cada proyecto Maven y construye sus imágenes Docker con los Dockerfiles correspondientes.
# Uso: bash configuration_scripts/build.sh [--no-cache] [--sin-maven]
#   --no-cache   construye las imágenes desde cero (tarda más)
#   --sin-maven  omite la compilación en el host y deja que Docker compile dentro de cada Dockerfile
# Requiere Docker Desktop corriendo y el archivo .env en la raíz.

set -u

# Clave para construir. Para cambiarla, edita CLAVE_BUILD.
CLAVE_BUILD="josesaid"

# Pide la clave 3 veces. Si alguna no coincide, no se construye nada.
verificar_clave() {
  local intento tecleada
  for intento in 1 2 3; do
    printf "🔐 Clave para construir (%s/3): " "$intento"
    tecleada=""
    read -r -s tecleada 2>/dev/null < /dev/tty || tecleada=""
    echo ""
    if [ "$tecleada" != "$CLAVE_BUILD" ]; then
      echo "❌ Clave incorrecta en el intento $intento. No se construyó nada."
      exit 1
    fi
  done
  echo "✅ Clave correcta. Construyendo..."
}
cd "$(dirname "$0")/.." || exit 1

TIMEOUT_DOCKER_SEG=${TIMEOUT_DOCKER_SEG:-180}

NO_CACHE=""
SIN_MAVEN=0
FORZAR=0
# Mismas omisiones de calidad que usa el Dockerfile de nexus-ms (spotbugs falla en el host)
SALTAR_CALIDAD=(-Dcheckstyle.skip -Dpmd.skip -Dspotbugs.skip -Djavadoc.skip -Djacoco.skip)
ayuda() {
cat <<'EOF'
Uso: bash configuration_scripts/build.sh [opciones]

Construye las imágenes Docker de EMPRESA_JOSESAID. Antes pide la clave 3 veces.

  Opción        Qué hace
  ------------  ---------------------------------------------------------------------
  --forzar      Reconstruye todas las imágenes, aunque ya existan
  --no-cache    Construye desde cero, sin caché de Docker (tarda más)
  --sin-maven   Omite la compilación en el host; Docker compila dentro de cada Dockerfile
  -h, --help    Muestra esta ayuda

Por defecto, las imágenes que ya existen se omiten por completo.
Con --forzar se reconstruyen todas. Si cambiaste código, usa --forzar.
EOF
}

for arg in "$@"; do
  case "$arg" in
    --no-cache) NO_CACHE="--no-cache" ;;
    --sin-maven) SIN_MAVEN=1 ;;
    --forzar) FORZAR=1 ;;
    -h|--help) ayuda; exit 0 ;;
    *) echo "Argumento desconocido: $arg"; echo "Uso: bash configuration_scripts/build.sh [--forzar] [--no-cache] [--sin-maven] [--help]"; exit 1 ;;
  esac
done

verificar_clave

# Colores solo si la salida es una terminal
if [ -t 1 ]; then
  RST=$'\033[0m'; NEG=$'\033[1m'; TENUE=$'\033[2m'
  ROJO=$'\033[1;31m'; VERDE=$'\033[1;32m'; AMARILLO=$'\033[1;33m'
  AZUL=$'\033[1;34m'; MAGENTA=$'\033[1;35m'; CIAN=$'\033[1;36m'
else
  RST=""; NEG=""; TENUE=""; ROJO=""; VERDE=""; AMARILLO=""; AZUL=""; MAGENTA=""; CIAN=""
fi

inicio=$(date +%s)
LOGS=$(mktemp -d)

formatear() {
  local s=$1
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}
transcurrido() { formatear $(( $(date +%s) - inicio )); }

linea() { echo "${MAGENTA}────────────────────────────────────────────────────────────${RST}"; }

paso() {
  echo ""
  linea
  echo "${NEG}${CIAN}$1${RST}"
  linea
}

fallo() {
  echo ""
  linea
  echo "${ROJO}❌ $1${RST}"
  echo "${AMARILLO}⏱️  Tiempo transcurrido: $(transcurrido)${RST}"
  linea
  rm -rf "$LOGS"
  exit 1
}

# Lista los servicios que tienen "build" en el compose indicado
lista_servicios() {
  docker compose "$@" config --format json 2>/dev/null | python3 -c '
import sys, json
d = json.load(sys.stdin)
for nombre, svc in d.get("services", {}).items():
    if "build" in svc:
        print(nombre)'
}

# Muestra el puntito amarillo y el cronómetro mientras un servicio se construye
animar_espera() {
  local svc="$1" inicio="$2" seg puntos
  while true; do
    seg=$(( $(date +%s) - inicio ))
    case $(( seg % 4 )) in
      0) puntos="" ;;
      1) puntos="." ;;
      2) puntos=".." ;;
      *) puntos="..." ;;
    esac
    printf "\r   ${AMARILLO}🐳 Construyendo %s%-3s ⏱️  %s${RST}   " "$svc" "$puntos" "$(formatear "$seg")"
    sleep 1
  done
}

# Nombre de la imagen de un servicio: el de "image:" o el nombre por defecto de Compose
imagen_de() {
  local svc="$1"
  shift
  docker compose "$@" config --format json 2>/dev/null | python3 -c '
import sys, json
d = json.load(sys.stdin)
svc = sys.argv[1]
img = d.get("services", {}).get(svc, {}).get("image")
print(img or (d.get("name", "") + "-" + svc))' "$svc"
}

# Construye los servicios de un compose uno por uno, mostrando el avance
construir_grupo() {
  local etiqueta=$1
  shift
  local servicios total n svc inicio_svc
  servicios=$(lista_servicios "$@")
  total=$(printf "%s\n" "$servicios" | grep -c . || true)
  if [ "$total" -eq 0 ]; then
    echo "   ${AMARILLO}⚠️  No hay servicios con build en $etiqueta${RST}"
    return 0
  fi
  echo "   ${AZUL}📦 Servicios a construir: ${NEG}$total${RST}"
  n=0
  while IFS= read -r svc; do
    [ -z "$svc" ] && continue
    n=$((n + 1))
    if [ "$FORZAR" = "0" ]; then
      img=$(imagen_de "$svc" "$@")
      if [ -n "$img" ] && docker image inspect "$img" >/dev/null 2>&1; then
        echo "   ♻️  [$n/$total] $svc: la imagen $img ya existe, se omite (usa --forzar para reconstruir)"
        continue
      fi
    fi
    echo "   ⏳ [$n/$total] Construyendo ${NEG}$svc${RST}${TENUE} ...${RST}"
    inicio_svc=$(date +%s)
    animar_espera "$svc" "$inicio_svc" &
    anim_pid=$!
    docker compose "$@" build $NO_CACHE --progress plain "$svc" > "$LOGS/$svc.build.log" 2>&1
    rc=$?
    kill "$anim_pid" 2>/dev/null
    wait "$anim_pid" 2>/dev/null
    printf "\r%-70s\r" ""
    if [ "$rc" -eq 0 ]; then
      echo "      ${VERDE}✅ $svc lista${RST} ${TENUE}($(formatear $(( $(date +%s) - inicio_svc ))))${RST}"
    else
      echo "      ${ROJO}❌ Falló $svc. Últimas líneas del log:${RST}"
      tail -n 20 "$LOGS/$svc.build.log" | sed 's/^/         /'
      return 1
    fi
  done <<< "$servicios"
  return 0
}

echo ""
echo "${NEG}${AZUL}🛠️  Construyendo EMPRESA_JOSESAID${RST}"

paso "🐳 Paso 1/6 · Verificando Docker y .env"
if docker info >/dev/null 2>&1; then
  echo "   ${VERDE}✅ Docker responde${RST}"
elif [ "$(uname -s)" = "Darwin" ]; then
  echo "   ${AMARILLO}🔧 Docker no está corriendo. Abriendo Docker Desktop...${RST}"
  open -a Docker 2>/dev/null || fallo "No se pudo abrir Docker Desktop. Ábrelo manualmente y vuelve a intentar."
  espera_docker=$(date +%s)
  while ! docker info >/dev/null 2>&1; do
    esperado_docker=$(( $(date +%s) - espera_docker ))
    if [ "$esperado_docker" -ge "$TIMEOUT_DOCKER_SEG" ]; then
      fallo "Docker Desktop no respondió en $(formatear "$TIMEOUT_DOCKER_SEG"). Revísalo manualmente."
    fi
    echo "   ${TENUE}⏳ [$(formatear "$esperado_docker")] Esperando a que Docker Desktop esté listo...${RST}"
    sleep 5
  done
  echo "   ${VERDE}✅ Docker Desktop listo (tardó $(formatear $(( $(date +%s) - espera_docker ))))${RST}"
else
  fallo "El daemon de Docker no responde. Abre Docker Desktop y vuelve a intentar."
fi
if [ ! -f .env ]; then
  fallo "No existe .env en la raíz del proyecto."
fi
echo "   ${VERDE}✅ .env encontrado${RST}"

paso "☕ Paso 2/6 · Compilando proyectos Maven en el host"
if [ "$SIN_MAVEN" = "1" ]; then
  echo "   ${TENUE}⏭️  Omitido (--sin-maven): Docker compilará dentro de cada Dockerfile${RST}"
elif ! command -v mvn >/dev/null 2>&1; then
  echo "   ${AMARILLO}⚠️  Maven no está instalado: se omite. Docker compilará dentro de cada Dockerfile${RST}"
else
  compilados=0
  fallidos=""
  # nexus-comun primero: nexus-ms depende de él
  proyectos=""
  if [ -f nexus-comun/pom.xml ]; then proyectos="nexus-comun"; fi
  for d in */; do
    d="${d%/}"
    [ "$d" = "nexus-comun" ] && continue
    [ -f "$d/pom.xml" ] && proyectos="$proyectos $d"
  done
  total=0
  for p in $proyectos; do total=$((total + 1)); done
  n=0
  for p in $proyectos; do
    n=$((n + 1))
    if [ "$p" = "nexus-comun" ]; then objetivo="install"; else objetivo="package"; fi
    echo "   ⏳ [$n/$total] Compilando ${NEG}$p${RST}${TENUE} (mvn $objetivo)...${RST}"
    if (cd "$p" && mvn -B -q "$objetivo" -DskipTests "${SALTAR_CALIDAD[@]}") > "$LOGS/$p.log" 2>&1; then
      echo "      ${VERDE}✅ $p: proyecto compilado${RST}"
      compilados=$((compilados + 1))
    else
      echo "      ${ROJO}❌ Falló la compilación. Últimas líneas del log:${RST}"
      tail -n 15 "$LOGS/$p.log" | sed 's/^/         /'
      fallidos="$fallidos $p"
    fi
  done
  echo ""
  echo "   ${AZUL}📦 Compilados: ${NEG}$compilados/$total${RST}"
  if [ -n "$fallidos" ]; then
    fallo "Falló la compilación de:$fallidos"
  fi
fi

paso "🐋 Paso 3/6 · Construyendo imágenes de la raíz (una por una)"
echo "   ${AMARILLO}⏳ Este paso es LENTO: cada imagen tarda entre 1 y 4 minutos.${RST}"
echo "   ${AMARILLO}   Ten paciencia. El punto y el tiempo avanzan mientras construye.${RST}"
echo "   ${AMARILLO}   No lo canceles: se ve quieto, pero está trabajando.${RST}"
if ! construir_grupo "raíz"; then
  fallo "Falló la construcción de una imagen de la raíz. Revisa el log mostrado arriba."
fi
echo "   ${VERDE}✅ Imágenes de la raíz listas${RST}"
echo "   📍 Ubicación: almacén local de Docker Desktop (no es una carpeta del proyecto)"
echo "   ${TENUE}   Para verlas: docker images --filter label=com.docker.compose.project=empresa${RST}"

paso "🐋 Paso 4/6 · Construyendo la imagen de ui-ms"
if ! construir_grupo "ui-ms" --env-file .env -f ui-ms/docker-compose.yml; then
  fallo "Falló la construcción de ui-ms. Revisa el log mostrado arriba."
fi
echo "   ${VERDE}✅ Imagen de ui-ms lista${RST}"

paso "🔍 Paso 5/6 · Verificando que existan todas las imágenes"
# Solo las imágenes que este script construye (servicios con "build"), no las de terceros como Kafka o Redis
extraer_construidas() {
  python3 -c 'import sys, json
d = json.load(sys.stdin)
for svc in d.get("services", {}).values():
    if "build" in svc and "image" in svc:
        print(svc["image"])'
}
imagenes=$( { docker compose config --format json 2>/dev/null | extraer_construidas; docker compose --env-file .env -f ui-ms/docker-compose.yml config --format json 2>/dev/null | extraer_construidas; } | sort -u)
faltan=0
ok=0
while IFS= read -r img; do
  [ -z "$img" ] && continue
  if docker image inspect "$img" >/dev/null 2>&1; then
    ok=$((ok + 1))
    printf "   ${VERDE}✅ %-40s${RST}\n" "$img"
  else
    faltan=$((faltan + 1))
    printf "   ${ROJO}❌ %-40s no existe${RST}\n" "$img"
  fi
done <<< "$imagenes"

paso "📚 Paso 6/6 · Generando reportes de calidad y documentación (docs/api)"
# Reportes de calidad y pruebas: el paso 2 los omite (-Dxxx.skip) para compilar rápido, así que se generan aquí.
# Ninguno bloquea el build: si una herramienta falla, la ficha solo queda sin resultados para ella.
if [ "$SIN_MAVEN" != "1" ] && command -v mvn >/dev/null 2>&1; then
  for d in */; do
    d="${d%/}"
    [ -f "$d/pom.xml" ] || continue
    echo "   ⏳ Reportes de calidad en $d..."
    if (cd "$d" && mvn -B -q test jacoco:report checkstyle:checkstyle pmd:pmd spotbugs:spotbugs javadoc:javadoc) > "$LOGS/reportes-$d.log" 2>&1; then
      echo "      ${VERDE}✅ pruebas, JaCoCo, Checkstyle, PMD, SpotBugs y Javadoc: $d${RST}"
    else
      echo "      ${AMARILLO}⚠️  Algún reporte falló en $d (log: $LOGS/reportes-$d.log)${RST}"
    fi
  done
fi
if command -v python3 >/dev/null 2>&1; then
  if PYTHONDONTWRITEBYTECODE=1 python3 configuration_scripts/generar-docs-api.py > "$LOGS/swagger.log" 2>&1; then
    echo "   ${VERDE}✅ Documentación generada en docs/api/index.html${RST}"
  else
    echo "   ${ROJO}❌ Falló la generación de Swagger. Últimas líneas del log:${RST}"
    tail -n 15 "$LOGS/swagger.log" | sed 's/^/      /'
    fallo "No se pudo generar la documentación Swagger"
  fi
else
  fallo "python3 no está instalado: no se puede generar la documentación Swagger"
fi

duracion=$(( $(date +%s) - inicio ))
echo ""
linea
if [ "$faltan" -gt 0 ]; then
  echo "${ROJO}❌ Faltan $faltan imágenes${RST}"
  echo "${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
  linea
  rm -rf "$LOGS"
  exit 1
fi
echo "${NEG}${VERDE}🏁 Build terminado: $ok imágenes listas${RST}"
echo "${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
echo "${TENUE}Ahora puedes ejecutar: bash configuration_scripts/start.sh${RST}"
linea
rm -rf "$LOGS"
