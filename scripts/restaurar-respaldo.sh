#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Uso: ./scripts/restaurar-respaldo.sh backups/archivo.dump"
  exit 1
fi

archivo="$1"
if [ ! -f "$archivo" ]; then
  echo "No existe el respaldo: $archivo"
  exit 1
fi

docker compose exec -T postgres pg_restore \
  --clean --if-exists --no-owner \
  -U rectificadora -d rectificadora < "$archivo"

echo "Respaldo restaurado correctamente."
