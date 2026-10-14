#!/usr/bin/env bash

# ==============================================================================
# Script per aturar tots els serveis locals (Frontend, Backend, PostgreSQL)
# ==============================================================================

PROJECT_DIR="/home/jautursis/Escriptori"
FRONT_DIR="${PROJECT_DIR}/FRONT-PROJECT-JJAUX"
BACK_DIR="${PROJECT_DIR}/BACK-PROJECT-JJAUX"
PG_DIR="/home/jautursis/.gemini/antigravity/brain/ac12e9c2-506a-4129-bb08-8c851cfd0667/scratch/pg"
PG_BIN="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/bin"
PG_LIB="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/lib"
PG_DATA="${PG_DIR}/data"

echo "Aturant serveis de JJAUX..."

# 1. Aturar Frontend (port 4300)
if [ -f "${FRONT_DIR}/frontend.pid" ]; then
    kill $(cat "${FRONT_DIR}/frontend.pid") 2>/dev/null || true
    rm -f "${FRONT_DIR}/frontend.pid"
fi
fuser -k 4300/tcp 2>/dev/null || true
echo "  [✓] Frontend aturat."

# 2. Aturar Backend (port 8080)
if [ -f "${BACK_DIR}/backend.pid" ]; then
    kill $(cat "${BACK_DIR}/backend.pid") 2>/dev/null || true
    rm -f "${BACK_DIR}/backend.pid"
fi
fuser -k 8080/tcp 2>/dev/null || true
echo "  [✓] Backend aturat."

# 3. Aturar PostgreSQL (port 5432)
if [ -d "${PG_DATA}" ]; then
    LD_LIBRARY_PATH="${PG_LIB}" "${PG_BIN}/pg_ctl" -D "${PG_DATA}" stop 2>/dev/null || true
fi
fuser -k 5432/tcp 2>/dev/null || true
echo "  [✓] PostgreSQL aturat."

echo "Tots els serveis han estat aturats correctament."

