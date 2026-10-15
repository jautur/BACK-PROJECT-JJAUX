#!/usr/bin/env bash

# ==============================================================================
# Script genèric per buidar la base de dades (JJAUX / AVISA)
# Buida tasques, incidències, ofertes i avisos
# Manté únicament els 3 usuaris base: admin, cli i pro
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if command -v node >/dev/null 2>&1; then
    node "${SCRIPT_DIR}/buidar_db.js" "$@"
elif command -v psql >/dev/null 2>&1; then
    echo "Executant buidat amb psql..."
    psql -h localhost -p 5432 -U postgres -d jjaux_db -f "${SCRIPT_DIR}/buidar_db.sql"
else
    echo "❌ Cal tenir Node.js o psql instal·lat per executar el script."
    exit 1
fi

