#!/usr/bin/env bash
# Compila cada proyecto Maven y construye sus imágenes Docker con los Dockerfiles correspondientes.
# Uso: bash build.sh [--no-cache] [--sin-maven]
#   --no-cache   construye las imágenes desde cero (tarda más)
#   --sin-maven  omite la compilación en el host y deja que Docker compile dentro de cada Dockerfile
# Requiere Docker Desktop corriendo y el archivo .env en la raíz.

set -u
cd "$(dirname "$0")" || exit 1

NO_CACHE=""
SIN_MAVEN=0
for arg in "$@"; do
  case "$arg" in
    --no-cache) NO_CACHE="--no-cache" ;;
    --sin-maven) SIN_MAVEN=1 ;;
    *) echo "Argumento desconocido: $arg"; echo "Uso: bash build.sh [--no-cache] [--sin-maven]"; exit 1 ;;
  esac
done

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

echo ""
echo "${NEG}${AZUL}🛠️  Construyendo EMPRESA_JOSESAID${RST}"

paso "🐳 Paso 1/5 · Verificando Docker y .env"
if ! docker info >/dev/null 2>&1; then
  fallo "El daemon de Docker no responde. Abre Docker Desktop y vuelve a intentar."
fi
echo "   ${VERDE}✅ Docker responde${RST}"
if [ ! -f .env ]; then
  fallo "No existe .env en la raíz del proyecto."
fi
echo "   ${VERDE}✅ .env encontrado${RST}"

paso "☕ Paso 2/5 · Compilando proyectos Maven en el host"
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
    echo "   🔨 [$n/$total] $p ${TENUE}(mvn $objetivo)${RST}"
    if (cd "$p" && mvn -B -q "$objetivo" -DskipTests) > "$LOGS/$p.log" 2>&1; then
      echo "      ${VERDE}✅ Compilado${RST}"
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

paso "🐋 Paso 3/5 · Construyendo imágenes Docker (raíz)"
echo "   ${TENUE}Servicios incluidos en el docker-compose.yml de la raíz${RST}"
if docker compose build $NO_CACHE --progress plain > "$LOGS/build-raiz.log" 2>&1; then
  echo "   ${VERDE}✅ Imágenes de la raíz construidas${RST}"
else
  echo "   ${ROJO}❌ Falló la construcción. Últimas líneas del log:${RST}"
  tail -n 25 "$LOGS/build-raiz.log" | sed 's/^/      /'
  fallo "docker compose build falló en la raíz"
fi

paso "🐋 Paso 4/5 · Construyendo imágenes Docker (ui-ms)"
if docker compose --env-file .env -f ui-ms/docker-compose.yml build $NO_CACHE --progress plain > "$LOGS/build-ui.log" 2>&1; then
  echo "   ${VERDE}✅ Imagen de ui-ms construida${RST}"
else
  echo "   ${ROJO}❌ Falló la construcción de ui-ms. Últimas líneas del log:${RST}"
  tail -n 25 "$LOGS/build-ui.log" | sed 's/^/      /'
  fallo "docker compose build falló en ui-ms"
fi

paso "🔍 Paso 5/5 · Verificando que existan todas las imágenes"
imagenes=$( (docker compose config --images; docker compose --env-file .env -f ui-ms/docker-compose.yml config --images) 2>/dev/null | sort -u)
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
echo "${TENUE}Ahora puedes ejecutar: ./start.sh${RST}"
linea
rm -rf "$LOGS"
