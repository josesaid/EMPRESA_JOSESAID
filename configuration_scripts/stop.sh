#!/usr/bin/env bash
# stop.sh: apaga o reinicia EMPRESA_JOSESAID; consulta su estado y limpia con flags.
# Uso: bash configuration_scripts/stop.sh --help
# Compatible con bash 3.2 (el que trae macOS).

set -u
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ" || exit 1

ARCHIVOS_COMPOSE=(-f docker-compose.yml -f ui-ms/docker-compose.yml)
ETIQUETA_PROYECTO="com.docker.compose.project=empresa"

if [ -t 1 ]; then
  RST=$'\033[0m'; NEG=$'\033[1m'; TENUE=$'\033[2m'
  ROJO=$'\033[1;31m'; VERDE=$'\033[1;32m'; AMARILLO=$'\033[1;33m'
  AZUL=$'\033[1;34m'; MAGENTA=$'\033[1;35m'; CIAN=$'\033[1;36m'
else
  RST=""; NEG=""; TENUE=""; ROJO=""; VERDE=""; AMARILLO=""; AZUL=""; MAGENTA=""; CIAN=""
fi

DRY_RUN=0
OPT_LOGS=0
OPT_IMAGENES=0
OPT_TARGETS=0
OPT_CACHE=0
OPT_CERRAR=0
MODO=""
SERVICIO=""
ERRORES=0

ayuda() {
cat <<'EOF'
Uso: bash configuration_scripts/stop.sh [opciones]

Apaga los servicios de EMPRESA_JOSESAID (incluye ui-ms). Por defecto conserva los volúmenes.

  Opción                 Qué hace                                                    Riesgo
  ---------------------  ----------------------------------------------------------  --------
  🟢 (sin opciones)      Apaga contenedores y redes del proyecto                     Ninguno
  🔁 --reiniciar         Apaga todo y lo vuelve a levantar con start.sh              Bajo
  🎯 --servicio NOMBRE   Detiene solo un servicio (ej. clientes-ms)                  Bajo
  🩺 --estado            Muestra servicios, salud y puertos (solo lectura)           Ninguno
  📜 --logs-servicio N   Últimas 100 líneas de log de un servicio (solo lectura)     Ninguno
  💽 --espacio           Muestra el espacio que usa Docker (solo lectura)            Ninguno
  🔍 -n, --dry-run       Muestra lo que haría sin ejecutar nada                      Ninguno
  🧾 --logs              Borra .verify-*.log y .build-*.log de la raíz               Bajo
  🧱 --imagenes          Quita imágenes colgadas del proyecto                        Bajo
  📁 --targets           Borra las carpetas target/ de Maven                         Bajo
  🧽 --cache             Borra la caché de build de Docker                           Bajo
  🧹 --limpiar           Atajo: --logs --imagenes --targets (no toca datos)          Bajo
  🐳 --cerrar-docker     Cierra Docker Desktop al terminar                           Bajo
  ❓ -h, --help          Muestra esta ayuda                                          -

Reglas:
  - --estado, --espacio y --logs-servicio van solos.
  - Solo una acción a la vez: sin opción, --reiniciar, --servicio.
  - --servicio no se combina con limpieza, y --reiniciar no se combina con --cerrar-docker.

Ejemplos:
  bash configuration_scripts/stop.sh --estado
  bash configuration_scripts/stop.sh --logs-servicio clientes-ms
  bash configuration_scripts/stop.sh --servicio clientes-ms
  bash configuration_scripts/stop.sh --reiniciar --limpiar
  bash configuration_scripts/stop.sh --dry-run --limpiar
EOF
}

error_uso() {
  echo "${ROJO}❌ $1${RST}" >&2
  echo "Usa --help para ver las opciones." >&2
  exit 2
}

fijar_modo() {
  if [ -n "$MODO" ] && [ "$MODO" != "$1" ]; then
    error_uso "Solo una acción a la vez (ya elegiste --$MODO)."
  fi
  MODO="$1"
}

while [ $# -gt 0 ]; do
  case "$1" in
    -n|--dry-run)    DRY_RUN=1 ;;
    --logs)          OPT_LOGS=1 ;;
    --imagenes)      OPT_IMAGENES=1 ;;
    --targets)       OPT_TARGETS=1 ;;
    --limpiar)       OPT_LOGS=1; OPT_IMAGENES=1; OPT_TARGETS=1 ;;
    --cache)         OPT_CACHE=1 ;;
    --cerrar-docker) OPT_CERRAR=1 ;;
    --estado)        fijar_modo estado ;;
    --espacio)       fijar_modo espacio ;;
    --reiniciar)     fijar_modo reiniciar ;;
    --servicio)
      fijar_modo servicio
      shift; [ $# -eq 0 ] && error_uso "--servicio necesita un nombre."
      SERVICIO="$1" ;;
    --logs-servicio)
      fijar_modo logs-servicio
      shift; [ $# -eq 0 ] && error_uso "--logs-servicio necesita un nombre."
      SERVICIO="$1" ;;
    -h|--help)       ayuda; exit 0 ;;
    *)               error_uso "Opción desconocida: $1" ;;
  esac
  shift
done

[ -z "$MODO" ] && MODO="apagar"
LIMPIEZA=$(( OPT_LOGS + OPT_IMAGENES + OPT_TARGETS + OPT_CACHE ))

case "$MODO" in
  estado|espacio|logs-servicio|servicio)
    if [ "$LIMPIEZA" -gt 0 ] || [ "$OPT_CERRAR" = 1 ]; then
      error_uso "--$MODO no se combina con limpieza ni con --cerrar-docker."
    fi ;;
  reiniciar)
    if [ "$OPT_CERRAR" = 1 ]; then
      error_uso "--reiniciar no se combina con --cerrar-docker."
    fi ;;
esac

linea() { echo "${MAGENTA}────────────────────────────────────────────────────────────${RST}"; }
paso() { echo ""; linea; echo "${NEG}${CIAN}$1${RST}"; linea; }
ejecutar() {
  if [ "$DRY_RUN" = 1 ]; then
    echo "   ${TENUE}🔍 [dry-run] $*${RST}"
    return 0
  fi
  "$@"
}
hecho() {
  if [ "$DRY_RUN" = 1 ]; then
    echo "   ${TENUE}🔍 (simulado) $1${RST}"
  else
    echo "   ${VERDE}✅ $1${RST}"
  fi
}
docker_activo() { docker info >/dev/null 2>&1; }
compose() { docker compose "${ARCHIVOS_COMPOSE[@]}" "$@"; }
requerir_docker() {
  if ! docker_activo; then
    echo "   ${ROJO}❌ Docker no responde. Abre Docker Desktop y vuelve a intentar.${RST}"
    ERRORES=$((ERRORES + 1))
    return 1
  fi
  return 0
}
servicio_existe() {
  compose config --services 2>/dev/null | grep -qx "$1"
}

# ---------- Consultas (solo lectura) ----------

modo_estado() {
  paso "🩺 Estado de EMPRESA_JOSESAID"
  requerir_docker || return
  total_estado=$(compose ps -a -q 2>/dev/null | grep -c '[^[:space:]]' || true)
  if [ "$total_estado" = "0" ]; then
    echo "   ${TENUE}ℹ️  No hay contenedores del proyecto (ni corriendo ni detenidos).${RST}"
    return
  fi
  compose ps -a
}

modo_espacio() {
  paso "💽 Espacio que usa Docker"
  requerir_docker || return
  docker system df
}

modo_logs_servicio() {
  paso "📜 Últimas 100 líneas de $SERVICIO"
  requerir_docker || return
  if ! servicio_existe "$SERVICIO"; then
    echo "   ${ROJO}❌ No existe el servicio: $SERVICIO${RST}"
    echo "   ${TENUE}Servicios disponibles: $(compose config --services 2>/dev/null | tr '\n' ' ')${RST}"
    ERRORES=$((ERRORES + 1)); return
  fi
  compose logs --tail 100 "$SERVICIO"
}

modo_servicio() {
  paso "🎯 Deteniendo solo $SERVICIO"
  requerir_docker || return
  if ! servicio_existe "$SERVICIO"; then
    echo "   ${ROJO}❌ No existe el servicio: $SERVICIO${RST}"
    echo "   ${TENUE}Servicios disponibles: $(compose config --services 2>/dev/null | tr '\n' ' ')${RST}"
    ERRORES=$((ERRORES + 1)); return
  fi
  if ejecutar compose stop "$SERVICIO"; then
    hecho "$SERVICIO detenido"
  else
    echo "   ${ROJO}❌ No se pudo detener $SERVICIO${RST}"; ERRORES=$((ERRORES + 1))
  fi
}

# ---------- Acciones de apagado ----------

apagar_todo() {
  paso "⏹️  Paso 1 · Apagando servicios"
  if ! docker_activo; then
    echo "   ${TENUE}ℹ️  Docker no responde: no hay servicios que apagar.${RST}"
    return
  fi
  if ejecutar compose down --remove-orphans; then
    hecho "Servicios detenidos (volúmenes conservados)"
  else
    echo "   ${ROJO}❌ docker compose down falló${RST}"; ERRORES=$((ERRORES + 1))
  fi
  # ui-ms es un proyecto Compose aparte: se detiene con su propio nombre de proyecto
  if ejecutar docker compose --env-file .env -p ui-ms -f ui-ms/docker-compose.yml down --remove-orphans; then
    hecho "UI (ui-ms) detenida"
  else
    echo "   ${AMARILLO}⚠️  No se pudo detener ui-ms (puede que ya estuviera apagada)${RST}"
  fi
  if [ "$DRY_RUN" = 0 ]; then
    restantes=$(docker ps -q --filter "label=$ETIQUETA_PROYECTO" | wc -l | tr -d ' ')
    if [ "$restantes" = "0" ]; then
      echo "   ${VERDE}✅ No quedan contenedores de EMPRESA_JOSESAID${RST}"
    else
      echo "   ${AMARILLO}⚠️  Quedan $restantes contenedores del proyecto: revisa con docker ps${RST}"
      ERRORES=$((ERRORES + 1))
    fi
    otros=$(docker ps --format '{{.Label "com.docker.compose.project"}}' | grep -v '^empresa$' | grep -v '^$' | sort -u | tr '\n' ' ')
    if [ -n "$otros" ]; then
      echo "   ${TENUE}ℹ️  Otros proyectos Compose siguen activos (no se tocaron): $otros${RST}"
    fi
  fi
}

limpiar_cache() {
  paso "🧽 Borrando caché de build de Docker"
  if ! docker_activo; then echo "   ${TENUE}ℹ️  Docker no responde: omitido.${RST}"; return; fi
  if ejecutar docker builder prune -f >/dev/null 2>&1; then
    hecho "Caché de build borrada"
  else
    echo "   ${AMARILLO}⚠️  No se pudo borrar la caché${RST}"; ERRORES=$((ERRORES + 1))
  fi
}

# Candado: pide la clave 3 veces antes de eliminar imágenes
pedir_clave_imagenes() {
  local clave="josesaid" intento tecleada
  echo "   🔐 Para eliminar imágenes debes escribir la clave 3 veces."
  for intento in 1 2 3; do
    printf "   🔐 Clave (%s/3): " "$intento"
    tecleada=""
    read -r -s tecleada 2>/dev/null < /dev/tty || tecleada=""
    echo ""
    if [ "$tecleada" != "$clave" ]; then
      echo "   ${ROJO}❌ Clave incorrecta en el intento $intento de 3.${RST}"
      echo "   ${ROJO}🛑 No se eliminó ninguna imagen. El script se detiene aquí.${RST}"
      exit 1
    fi
    echo "   ${VERDE}✅ Intento $intento de 3 correcto${RST}"
  done
  echo "   ${VERDE}🔓 Clave verificada. Se continúa con la eliminación.${RST}"
}

limpiar_imagenes() {
  paso "🧱 Quitando imágenes colgadas del proyecto"
  if ! docker_activo; then
    echo "   ${TENUE}ℹ️  Docker no responde: omitido.${RST}"
    return
  fi
  echo "   🔎 Buscando imágenes colgadas del proyecto..."
  ids=$(docker images -q --filter dangling=true --filter "label=$ETIQUETA_PROYECTO")
  n=$(printf '%s\n' "$ids" | grep -c '[^[:space:]]')
  if [ "$n" = "0" ]; then
    echo "   ${TENUE}ℹ️  No hay imágenes colgadas. Nada que eliminar.${RST}"
    return
  fi
  echo "   ${AMARILLO}📦 Imágenes colgadas encontradas: $n${RST}"
  if [ "$DRY_RUN" = 1 ]; then
    echo "   ${TENUE}🔍 [dry-run] Se pediría la clave 3 veces antes de eliminar.${RST}"
    echo "   ${TENUE}🔍 [dry-run] Se eliminarían $n imágenes.${RST}"
    return
  fi
  pedir_clave_imagenes
  echo "   🗑️  Eliminando $n imágenes. Puede tardar un poco..."
  # shellcheck disable=SC2086
  if docker rmi $ids >/dev/null 2>&1; then
    hecho "Imágenes eliminadas: $n"
  else
    echo "   ${AMARILLO}⚠️  Algunas no se pudieron eliminar (puede que sigan en uso).${RST}"
    ERRORES=$((ERRORES + 1))
  fi
}

limpiar_logs() {
  paso "🧾 Borrando logs de verificación y build"
  shopt -s nullglob
  logs=(.verify-*.log .build-*.log)
  shopt -u nullglob
  if [ "${#logs[@]}" -eq 0 ]; then
    echo "   ${TENUE}ℹ️  No hay logs que borrar.${RST}"
    return
  fi
  kb=$(du -ck "${logs[@]}" | tail -1 | awk '{print $1}')
  echo "   ${AMARILLO}📄 Logs: ${#logs[@]} archivos · ${kb} KB${RST}"
  if ejecutar rm -f -- "${logs[@]}"; then
    hecho "Logs eliminados"
  else
    echo "   ${ROJO}❌ No se pudieron borrar los logs${RST}"; ERRORES=$((ERRORES + 1))
  fi
}

limpiar_targets() {
  paso "📁 Borrando carpetas target/ de Maven"
  if [ "$DRY_RUN" = 1 ]; then
    echo "   ${TENUE}🔍 [dry-run] bash rm-target-directories.sh $RAIZ${RST}"
  elif [ -f "$RAIZ/rm-target-directories.sh" ]; then
    if bash "$RAIZ/rm-target-directories.sh" "$RAIZ"; then
      hecho "Targets borrados"
    else
      echo "   ${ROJO}❌ rm-target-directories.sh terminó con error${RST}"; ERRORES=$((ERRORES + 1))
    fi
  else
    echo "   ${ROJO}❌ No existe rm-target-directories.sh en la raíz${RST}"; ERRORES=$((ERRORES + 1))
  fi
}

reiniciar_todo() {
  paso "🔁 Levantando de nuevo con start.sh"
  if ejecutar bash "$RAIZ/start.sh"; then
    hecho "Proyecto levantado"
  else
    echo "   ${ROJO}❌ start.sh terminó con error${RST}"; ERRORES=$((ERRORES + 1))
  fi
}

cerrar_docker() {
  paso "🐳 Cerrando Docker Desktop"
  if [ "$(uname -s)" != "Darwin" ]; then
    echo "   ${TENUE}ℹ️  Omitido: solo aplica en macOS.${RST}"
    return
  fi
  if ! docker_activo; then
    hecho "Docker ya estaba cerrado"
    return
  fi
  if [ "$DRY_RUN" = 1 ]; then
    echo "   ${TENUE}🔍 [dry-run] osascript -e 'quit app \"Docker\"' y esperar a que Docker deje de responder${RST}"
    hecho "Docker Desktop cerrado"
    return
  fi
  osascript -e 'quit app "Docker"' >/dev/null 2>&1 || true
  espera=0
  while docker_activo; do
    if [ "$espera" -ge 60 ]; then
      echo "   ${ROJO}❌ Docker sigue respondiendo tras 60 s. Ciérralo desde la barra de menú.${RST}"
      ERRORES=$((ERRORES + 1))
      return
    fi
    sleep 2
    espera=$((espera + 2))
  done
  hecho "Docker Desktop cerrado"
}

# ---------- Ejecución ----------

echo ""
if [ "$DRY_RUN" = 1 ]; then
  echo "${NEG}${AMARILLO}🔍 MODO PRUEBA (dry-run): no se ejecuta nada${RST}"
fi
echo "${NEG}${ROJO}🛑 EMPRESA_JOSESAID · acción: $MODO${RST}"

case "$MODO" in
  estado)        modo_estado ;;
  espacio)       modo_espacio ;;
  logs-servicio) modo_logs_servicio ;;
  servicio)      modo_servicio ;;
  apagar)        apagar_todo ;;
  reiniciar)     apagar_todo ;;
esac

if [ "$MODO" = "apagar" ] || [ "$MODO" = "reiniciar" ]; then
  [ "$OPT_CACHE" = 1 ]     && limpiar_cache
  [ "$OPT_IMAGENES" = 1 ]  && limpiar_imagenes
  [ "$OPT_LOGS" = 1 ]      && limpiar_logs
  [ "$OPT_TARGETS" = 1 ]   && limpiar_targets
  [ "$MODO" = "reiniciar" ] && reiniciar_todo
  [ "$OPT_CERRAR" = 1 ]    && cerrar_docker
fi

echo ""
linea
echo "${NEG}📋 Resumen${RST}"
if [ "$DRY_RUN" = 1 ]; then
  echo "   ${AMARILLO}🔍 Nada se ejecutó: revisa los [dry-run] de arriba${RST}"
elif [ "$ERRORES" = "0" ]; then
  echo "   ${VERDE}✅ Terminado sin errores${RST}"
else
  echo "   ${AMARILLO}⚠️  Terminado con $ERRORES pendiente(s): revisa los pasos anteriores${RST}"
fi
if [ "$MODO" = "apagar" ] || [ "$MODO" = "reiniciar" ]; then
  echo "   ${AZUL}💾 Volúmenes conservados${RST}"
fi
echo "${NEG}${ROJO}🏁 Fin${RST}"
linea

[ "$ERRORES" = "0" ] && exit 0 || exit 1
