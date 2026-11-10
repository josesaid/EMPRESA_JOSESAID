#!/usr/bin/env bash
# Borra los directorios target (salida de Maven) de cada microservicio bajo una ruta.
# Solo borra un target si su carpeta padre tiene pom.xml, para no tocar otros directorios con ese nombre.
# Uso: bash configuration_scripts/rm-target-directories.sh [ruta]
#      Sin ruta usa la raíz del proyecto.

set -u
RAIZ="${1:-$(cd "$(dirname "$0")/.." && pwd)}"

# Colores solo si la salida es una terminal
if [ -t 1 ]; then
  RST=$'\033[0m'; NEG=$'\033[1m'; TENUE=$'\033[2m'
  ROJO=$'\033[1;31m'; VERDE=$'\033[1;32m'; AMARILLO=$'\033[1;33m'
  AZUL=$'\033[1;34m'; MAGENTA=$'\033[1;35m'; CIAN=$'\033[1;36m'
else
  RST=""; NEG=""; TENUE=""; ROJO=""; VERDE=""; AMARILLO=""; AZUL=""; MAGENTA=""; CIAN=""
fi

inicio=$(date +%s)

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
  exit 1
}

kb_a_texto() {
  local kb=$1
  if [ "$kb" -ge 1024 ]; then
    awk "BEGIN{printf \"%.1f MB\", $kb/1024}"
  else
    echo "${kb} KB"
  fi
}

echo ""
echo "${NEG}${ROJO}🧹 Limpieza de directorios target${RST}"
echo "${TENUE}   Ruta: $RAIZ${RST}"

paso "📂 Paso 1/4 · Verificando la ruta"
if [ ! -d "$RAIZ" ]; then
  fallo "La ruta no existe: $RAIZ"
fi
echo "   ${VERDE}✅ Ruta válida${RST}"

paso "🔎 Paso 2/4 · Buscando directorios target"
candidatos=$(find "$RAIZ" -type d -name target -prune 2>/dev/null)
a_borrar=""
omitidos=0
total_candidatos=0
while IFS= read -r dir; do
  [ -z "$dir" ] && continue
  total_candidatos=$((total_candidatos + 1))
  if [ -f "$(dirname "$dir")/pom.xml" ]; then
    a_borrar="${a_borrar}${dir}"$'\n'
  else
    omitidos=$((omitidos + 1))
    echo "   ${AMARILLO}⏭️  Omitido (sin pom.xml en el padre): ${dir#"$RAIZ"/}${RST}"
  fi
done <<< "$candidatos"

cantidad=0
while IFS= read -r dir; do
  [ -z "$dir" ] && continue
  cantidad=$((cantidad + 1))
done <<< "$a_borrar"

echo "   ${AZUL}📦 Encontrados: ${NEG}$total_candidatos${RST}${AZUL} · Para borrar: ${NEG}$cantidad${RST}${AZUL} · Omitidos: $omitidos${RST}"

if [ "$cantidad" -eq 0 ]; then
  paso "🗑️  Paso 3/4 · Borrando"
  echo "   ${TENUE}ℹ️  No hay directorios target que borrar${RST}"
else
  paso "🗑️  Paso 3/4 · Borrando directorios target"
fi

borrados=0
kb_total=0
while IFS= read -r dir; do
  [ -z "$dir" ] && continue
  kb=$(du -sk "$dir" 2>/dev/null | awk '{print $1}')
  kb=${kb:-0}
  if rm -rf "$dir" 2>/dev/null; then
    borrados=$((borrados + 1))
    kb_total=$((kb_total + kb))
    echo "   ${VERDE}✅ ${dir#"$RAIZ"/}${RST}  ${TENUE}($(kb_a_texto "$kb"))${RST}"
  else
    echo "   ${ROJO}❌ No se pudo borrar: ${dir#"$RAIZ"/}${RST}"
  fi
done <<< "$a_borrar"

paso "🧪 Paso 4/4 · Verificando que no queden targets"
restantes=$(find "$RAIZ" -type d -name target -prune -exec sh -c '[ -f "$(dirname "$1")/pom.xml" ] && echo "$1"' _ {} \; 2>/dev/null | wc -l | tr -d ' ')
if [ "$restantes" = "0" ]; then
  echo "   ${VERDE}✅ No quedan targets de microservicios${RST}"
else
  echo "   ${AMARILLO}⚠️  Quedan $restantes targets. Revisa los errores anteriores${RST}"
fi

duracion=$(( $(date +%s) - inicio ))
echo ""
linea
echo "${NEG}📋 Resumen${RST}"
echo "   ${VERDE}🗑️  Borrados: $borrados${RST}"
echo "   ${AZUL}💾 Espacio liberado: $(kb_a_texto "$kb_total")${RST}"
echo "   ${AMARILLO}⏭️  Omitidos: $omitidos${RST}"
echo "   ${AZUL}⏱️  Tiempo total: ${NEG}$(formatear "$duracion")${RST}${AZUL}  ($duracion s)${RST}"
echo "${NEG}${ROJO}🏁 Limpieza terminada${RST}"
linea
