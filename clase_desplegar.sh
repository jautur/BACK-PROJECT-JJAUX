#!/usr/bin/env bash

# ==============================================================================
# Script de desplegament ràpid en local per al projecte JJAUX (Versió Web)
# Inicia:
#   1. PostgreSQL (Port 5432 / Base de dades jjaux_db)
#   2. Backend Spring Boot (Port 8080)
#   3. Frontend Angular (Port 4300)
#   4. Obertura automàtica al navegador web
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "${SCRIPT_DIR}/../BACK-PROJECT-JJAUX" ]; then
    FRONT_DIR="${SCRIPT_DIR}"
    BACK_DIR="$(cd "${SCRIPT_DIR}/../BACK-PROJECT-JJAUX" && pwd)"
elif [ -d "${SCRIPT_DIR}/FRONT-PROJECT-JJAUX" ]; then
    FRONT_DIR="$(cd "${SCRIPT_DIR}/FRONT-PROJECT-JJAUX" && pwd)"
    BACK_DIR="$(cd "${SCRIPT_DIR}/BACK-PROJECT-JJAUX" && pwd)"
elif [ -d "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" ]; then
    FRONT_DIR="$(cd "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" && pwd)"
    BACK_DIR="${SCRIPT_DIR}"
else
    FRONT_DIR="/home/jautursis/Escriptori/FRONT-PROJECT-JJAUX"
    BACK_DIR="/home/jautursis/Escriptori/BACK-PROJECT-JJAUX"
fi

PG_DIR="/home/jautursis/.gemini/antigravity/brain/ac12e9c2-506a-4129-bb08-8c851cfd0667/scratch/pg"
PG_BIN="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/bin"
PG_LIB="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/lib"
PG_DATA="${PG_DIR}/data"

echo "=========================================================="
echo "          INICIANT DESPLEGAMENT WEB JJAUX"
echo "=========================================================="

# ------------------------------------------------------------------------------
# 1. Base de dades PostgreSQL (Port 5432)
# ------------------------------------------------------------------------------
echo -e "\n[1/3] Comprovant PostgreSQL..."
if nc -z 127.0.0.1 5432 2>/dev/null || node -e "const net = require('net'); const s = net.connect(5432, '127.0.0.1', () => { s.destroy(); process.exit(0); }); s.on('error', () => process.exit(1));" 2>/dev/null; then
    echo "  -> PostgreSQL ja està en marxa al port 5432."
else
    echo "  -> Engegant servidor PostgreSQL..."
    if [ -d "${PG_DATA}" ] && [ -x "${PG_BIN}/pg_ctl" ]; then
        LD_LIBRARY_PATH="${PG_LIB}" "${PG_BIN}/pg_ctl" -D "${PG_DATA}" -l "${PG_DIR}/postgres.log" -o "-p 5432" start || true
    elif command -v pg_ctlcluster >/dev/null 2>&1; then
        sudo service postgresql start 2>/dev/null || pg_ctlcluster 16 main start 2>/dev/null || true
    fi
    sleep 2
fi

# Inicialització de taules i dades si cal
if [ -f "${PG_DIR}/init_db.js" ]; then
    cd "${PG_DIR}" && node init_db.js >/dev/null 2>&1 || true
fi

# Assegurar usuari administrador (admin@admin.es / adminadmin)
echo "  -> Verificant usuari administrador (admin@admin.es)..."
if [ -f "${FRONT_DIR}/assegurar_admin.js" ]; then
    node "${FRONT_DIR}/assegurar_admin.js" || true
elif [ -f "${BACK_DIR}/assegurar_admin.js" ]; then
    node "${BACK_DIR}/assegurar_admin.js" || true
fi

# ------------------------------------------------------------------------------
# 2. Backend Spring Boot (Port 8080)
# ------------------------------------------------------------------------------
echo -e "\n[2/3] Comprovant Backend Spring Boot..."
if nc -z 127.0.0.1 8080 2>/dev/null || curl -s -o /dev/null http://localhost:8080/api/dashboard 2>/dev/null; then
    echo "  -> Backend ja està responent a http://localhost:8080"
else
    JAR_FILE="${BACK_DIR}/target/jjaux-back-0.0.1-SNAPSHOT.jar"
    if [ ! -f "${JAR_FILE}" ]; then
        echo "  -> Compilant el backend amb Maven..."
        cd "${BACK_DIR}"
        if [ -x "./mvnw" ]; then
            ./mvnw clean package -DskipTests
        elif command -v mvn >/dev/null 2>&1; then
            mvn clean package -DskipTests
        else
            /home/jautursis/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin/mvn clean package -DskipTests
        fi
    fi

    echo "  -> Engegant Spring Boot en segon pla..."
    fuser -k 8080/tcp 2>/dev/null || true
    cd "${BACK_DIR}"
    setsid nohup java -jar "${JAR_FILE}" > "${BACK_DIR}/backend.log" 2>&1 < /dev/null &
    BACKEND_PID=$!
    disown $BACKEND_PID 2>/dev/null || true
    echo $BACKEND_PID > "${BACK_DIR}/backend.pid"

    echo -n "  -> Esperant que el backend estigui llest..."
    for i in {1..35}; do
        if curl -s http://localhost:8080/api/dashboard >/dev/null 2>&1; then
            echo " Llest!"
            break
        fi
        echo -n "."
        sleep 1
    done
fi

# ------------------------------------------------------------------------------
# 3. Frontend Angular (Port 4300)
# ------------------------------------------------------------------------------
echo -e "\n[3/3] Comprovant Frontend Angular (Port 4300)..."
if nc -z 127.0.0.1 4300 2>/dev/null || curl -s -o /dev/null http://localhost:4300 2>/dev/null; then
    echo "  -> Frontend ja està servint a http://localhost:4300"
else
    if [ ! -d "${FRONT_DIR}/dist/front-project-jjaux-1/browser" ]; then
        echo "  -> Compilant aplicació Angular per a producció..."
        cd "${FRONT_DIR}" && npm run build
    fi

    echo "  -> Engegant servidor web del Frontend..."
    fuser -k 4300/tcp 2>/dev/null || true
    cd "${FRONT_DIR}"
    setsid nohup node serve.js > "${FRONT_DIR}/frontend.log" 2>&1 < /dev/null &
    FRONT_PID=$!
    disown $FRONT_PID 2>/dev/null || true
    echo $FRONT_PID > "${FRONT_DIR}/frontend.pid"
    sleep 2
fi

# ------------------------------------------------------------------------------
# 4. Obrir al navegador web
# ------------------------------------------------------------------------------
echo -e "\n  -> Obrint aplicació web al navegador..."
WEB_URL="http://localhost:4300/inici"
if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "${WEB_URL}" >/dev/null 2>&1 || true
elif command -v sensible-browser >/dev/null 2>&1; then
    sensible-browser "${WEB_URL}" >/dev/null 2>&1 || true
elif command -v cmd.exe >/dev/null 2>&1; then
    cmd.exe /c start "${WEB_URL}" 2>/dev/null || true
fi

echo "=========================================================="
echo "           DESPLEGAMENT WEB COMPLETAT AMB ÈXIT!"
echo "=========================================================="
echo " - Base de dades: PostgreSQL a localhost:5432 (BD: jjaux_db)"
echo " - API Backend:   http://localhost:8080/api"
echo " - Aplicació Web: http://localhost:4300/inici"
echo " - Usuari Admin:  admin@admin.es / adminadmin"
echo "=========================================================="
echo "Per aturar tots els serveis: ./aturar.sh"
echo "=========================================================="
