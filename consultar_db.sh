#!/bin/bash
# Wrapper bash per executar el script de consulta de la base de dades
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
node "$SCRIPT_DIR/consultar_db.js" "$@"
