#!/usr/bin/env bash

# ==============================================================================
# Script per aturar tots els serveis locals a CASA (Frontend, Backend)
# ==============================================================================

# Directoris del projecte a CASA
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_NAME="$(basename "${SCRIPT_DIR}")"

if [ "$DIR_NAME" = "BACK-PROJECT-JJAUX" ]; then
    BACK_DIR="${SCRIPT_DIR}"
    if [ -d "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" ]; then
        FRONT_DIR="$(cd "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" && pwd)"
    else
        FRONT_DIR="/c/Users/User/Desktop/FRONT-PROJECT-JJAUX"
    fi
elif [ "$DIR_NAME" = "FRONT-PROJECT-JJAUX" ]; then
    FRONT_DIR="${SCRIPT_DIR}"
    if [ -d "${SCRIPT_DIR}/../BACK-PROJECT-JJAUX" ]; then
        BACK_DIR="$(cd "${SCRIPT_DIR}/../BACK-PROJECT-JJAUX" && pwd)"
    else
        BACK_DIR="/c/Users/User/Desktop/BACK-PROJECT-JJAUX"
    fi
else
    if [ -d "${SCRIPT_DIR}/FRONT-PROJECT-JJAUX" ] && [ -d "${SCRIPT_DIR}/BACK-PROJECT-JJAUX" ]; then
        FRONT_DIR="$(cd "${SCRIPT_DIR}/FRONT-PROJECT-JJAUX" && pwd)"
        BACK_DIR="$(cd "${SCRIPT_DIR}/BACK-PROJECT-JJAUX" && pwd)"
    else
        FRONT_DIR="/c/Users/User/Desktop/FRONT-PROJECT-JJAUX"
        BACK_DIR="/c/Users/User/Desktop/BACK-PROJECT-JJAUX"
    fi
fi

echo "Aturant serveis de JJAUX a CASA..."

kill_service() {
    local pidfile="$1"
    local port="$2"

    if [ -f "$pidfile" ]; then
        local pid=$(cat "$pidfile" 2>/dev/null | tr -d '\r\n')
        if [ -n "$pid" ]; then
            taskkill //F //PID "$pid" >/dev/null 2>&1 || kill -9 "$pid" 2>/dev/null || true
        fi
        rm -f "$pidfile"
    fi

    # Alliberar port amb taskkill
    if command -v netstat >/dev/null 2>&1; then
        local pids=$(netstat -ano 2>/dev/null | grep ":${port} " | awk '{print $5}' | tr -d '\r' | sort -u | grep -v '^0$' || true)
        for p in $pids; do
            taskkill //F //PID "$p" >/dev/null 2>&1 || true
        done
    fi
}

# 1. Aturar Frontend (port 4300)
kill_service "${FRONT_DIR}/frontend.pid" 4300
echo "  [✓] Frontend aturat (port 4300)."

# 2. Aturar Backend (port 8080)
kill_service "${BACK_DIR}/backend.pid" 8080
echo "  [✓] Backend aturat (port 8080)."

# 3. PostgreSQL (port 5432)
echo "  [✓] PostgreSQL: Servei del sistema mantingut actiu."

echo "Tots els serveis han estat aturats correctament."
