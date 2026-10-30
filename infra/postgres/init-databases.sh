#!/bin/bash
# Crea un usuario y una base de datos por microservicio.
# Se ejecuta solo en la primera inicialización del volumen postgres-data.
set -euo pipefail

crear_servicio() {
  local servicio="$1"
  local usuario="$2"
  local clave="$3"

  psql -v ON_ERROR_STOP=1 \
       -v svc_db="$servicio" \
       -v svc_user="$usuario" \
       -v svc_pass="$clave" \
       --username "$POSTGRES_USER" --dbname postgres <<'EOSQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'svc_user', :'svc_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'svc_user') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'svc_db', :'svc_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'svc_db') \gexec
EOSQL
  echo "Base de datos '${servicio}' lista para el usuario '${usuario}'"
}

crear_servicio clientes clientes "${CLIENTES_DB_PASSWORD:?Falta CLIENTES_DB_PASSWORD}"
crear_servicio usuarios_accesos usuarios_accesos "${USUARIOS_ACCESOS_DB_PASSWORD:?Falta USUARIOS_ACCESOS_DB_PASSWORD}"
crear_servicio facturacion_pagos facturacion_pagos "${FACTURACION_DB_PASSWORD:?Falta FACTURACION_DB_PASSWORD}"
crear_servicio contabilidad contabilidad "${CONTABILIDAD_DB_PASSWORD:?Falta CONTABILIDAD_DB_PASSWORD}"
crear_servicio inventario inventario "${INVENTARIO_DB_PASSWORD:?Falta INVENTARIO_DB_PASSWORD}"
crear_servicio compras compras "${COMPRAS_DB_PASSWORD:?Falta COMPRAS_DB_PASSWORD}"
crear_servicio pedidos pedidos "${PEDIDOS_DB_PASSWORD:?Falta PEDIDOS_DB_PASSWORD}"
crear_servicio documentos documentos "${DOCUMENTOS_DB_PASSWORD:?Falta DOCUMENTOS_DB_PASSWORD}"
crear_servicio reportes reportes "${REPORTES_DB_PASSWORD:?Falta REPORTES_DB_PASSWORD}"
crear_servicio nexus nexus "${NEXUS_DB_PASSWORD:?Falta NEXUS_DB_PASSWORD}"
