#!/usr/bin/env bash

# ==============================================================================
# Script de desplegament ràpid en local per a CASA - JJAUX
# Inicia:
#   1. PostgreSQL (Port 5432 / Base de dades jjaux_db)
#   2. Backend Spring Boot (Port 8080)
#   3. Frontend Angular (Port 4300)
# ==============================================================================

set -e

# Assegurar accés a binaris en entorns Windows/Git Bash/WSL
export PATH="/c/Program Files/nodejs:/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin:/c/Program Files/PostgreSQL/18/bin:$PATH"

# Suport per a flags de reconstrucció manual
FORCE_REBUILD=false
if [ "$1" = "--rebuild" ] || [ "$1" = "-f" ] || [ "$1" = "--force" ]; then
    FORCE_REBUILD=true
fi

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

# Conversió a ruta Windows
to_win_path() {
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -w "$1"
    else
        echo "$1" | sed 's|^/c/|C:\\|' | sed 's|/|\\|g'
    fi
}

FRONT_WIN=$(to_win_path "${FRONT_DIR}")
BACK_WIN=$(to_win_path "${BACK_DIR}")

echo "=========================================================="
echo "          INICIANT DESPLEGAMENT LOCAL JJAUX (CASA)"
echo "=========================================================="

# Funció per alliberar un port si està bloquejat per processos antics
free_port() {
    local port="$1"
    if command -v netstat >/dev/null 2>&1; then
        local pids=$(netstat -ano 2>/dev/null | grep ":${port} " | awk '{print $5}' | tr -d '\r' | sort -u | grep -v '^0$' || true)
        for p in $pids; do
            taskkill //F //PID "$p" >/dev/null 2>&1 || true
        done
    fi
}

# ------------------------------------------------------------------------------
# 1. Base de dades PostgreSQL (Port 5432)
# ------------------------------------------------------------------------------
echo -e "\n[1/3] Comprovant PostgreSQL..."
if node -e "const net = require('net'); const s = net.connect(5432, '127.0.0.1', () => { s.destroy(); process.exit(0); }); s.on('error', () => process.exit(1));" 2>/dev/null; then
    echo "  -> PostgreSQL ja està en marxa al port 5432."
else
    echo "  -> Engegant servei PostgreSQL..."
    net start postgresql-x64-18 2>/dev/null || "/c/Program Files/PostgreSQL/18/bin/pg_ctl.exe" start -D "C:\\Program Files\\PostgreSQL\\18\\data" 2>/dev/null || true
    sleep 2
fi

# Inicialització i verificació de la base de dades jjaux_db
echo "  -> Verificant esquema i dades a jjaux_db..."
node -e "
let pg;
try { pg = require('pg'); } catch(e) { try { pg = require('${FRONT_DIR}/node_modules/pg'); } catch(e2){} }
if (!pg) process.exit(0);
const { Client } = pg;
(async () => {
  const c = new Client({ host: 'localhost', port: 5432, user: 'postgres', password: process.env.PGPASSWORD || 'postgres', database: 'postgres' });
  try {
    await c.connect();
    const res = await c.query(\"SELECT 1 FROM pg_database WHERE datname='jjaux_db'\");
    if (res.rows.length === 0) {
      await c.query('CREATE DATABASE jjaux_db');
      console.log('  -> Base de dades jjaux_db creada amb èxit.');
    }
  } catch(e) {
  } finally {
    try { await c.end(); } catch(e){}
    process.exit(0);
  }
})();
" 2>/dev/null || true

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
JAR_FILE="${BACK_DIR}/target/jjaux-back-0.0.1-SNAPSHOT.jar"
WIN_JAR_FILE="${BACK_WIN}\\target\\jjaux-back-0.0.1-SNAPSHOT.jar"
REBUILD_BACKEND=false

if [ "$FORCE_REBUILD" = "true" ] || [ ! -f "${JAR_FILE}" ]; then
    REBUILD_BACKEND=true
elif [ -n "$(cd "${BACK_DIR}" && find src pom.xml -newer "${JAR_FILE}" 2>/dev/null | head -n 1)" ]; then
    echo "  -> Canvis detectats al codi del Backend."
    REBUILD_BACKEND=true
fi

if [ "$REBUILD_BACKEND" = "true" ]; then
    free_port 8080
    echo "  -> Compilant el backend amb Maven..."
    cd "${BACK_DIR}"
    if [ -f "./mvnw.cmd" ]; then
        ./mvnw.cmd package -DskipTests
    else
        ./mvnw package -DskipTests
    fi
fi

if [ "$REBUILD_BACKEND" = "false" ] && curl -s -o /dev/null http://localhost:8080/api/dashboard 2>/dev/null; then
    echo "  -> Backend ja està responent a http://localhost:8080"
else
    echo "  -> Engegant Spring Boot en segon pla..."
    free_port 8080
    cd "${BACK_DIR}"
    export SPRING_DATASOURCE_PASSWORD="postgres"

    # Engegar procés desacoblat a Windows via PowerShell Start-Process amb registre de PID
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "\$p = Start-Process -FilePath 'java' -ArgumentList '-jar', '${WIN_JAR_FILE}' -WorkingDirectory '${BACK_WIN}' -RedirectStandardOutput '${BACK_WIN}\\backend.log' -RedirectStandardError '${BACK_WIN}\\backend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '${BACK_WIN}\\backend.pid' -Value \$p.Id" 2>/dev/null || {
        nohup java -jar "${JAR_FILE}" > "${BACK_DIR}/backend.log" 2>&1 &
        echo $! > "${BACK_DIR}/backend.pid"
    }

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
echo -e "\n[3/3] Comprovant Frontend Angular..."
DIST_INDEX="${FRONT_DIR}/dist/front-project-jjaux-1/browser/index.html"
REBUILD_FRONTEND=false

if [ "$FORCE_REBUILD" = "true" ] || [ ! -f "${DIST_INDEX}" ]; then
    REBUILD_FRONTEND=true
elif [ -n "$(cd "${FRONT_DIR}" && find src package.json -newer "${DIST_INDEX}" 2>/dev/null | head -n 1)" ]; then
    echo "  -> Canvis detectats al codi del Frontend."
    REBUILD_FRONTEND=true
fi

if [ "$REBUILD_FRONTEND" = "true" ]; then
    free_port 4300
    echo "  -> Compilant aplicació Angular..."
    cd "${FRONT_DIR}"
    if command -v npm.cmd >/dev/null 2>&1; then
        npm.cmd run build
    else
        npm run build
    fi
fi

if [ "$REBUILD_FRONTEND" = "false" ] && curl -s -o /dev/null http://localhost:4300 2>/dev/null; then
    echo "  -> Frontend ja està servint a http://localhost:4300"
else
    echo "  -> Engegant servidor web del Frontend (port 4300)..."
    free_port 4300
    cd "${FRONT_DIR}"

    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "\$p = Start-Process -FilePath 'node' -ArgumentList 'serve.js' -WorkingDirectory '${FRONT_WIN}' -RedirectStandardOutput '${FRONT_WIN}\\frontend.log' -RedirectStandardError '${FRONT_WIN}\\frontend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '${FRONT_WIN}\\frontend.pid' -Value \$p.Id" 2>/dev/null || {
        nohup node serve.js > "${FRONT_DIR}/frontend.log" 2>&1 &
        echo $! > "${FRONT_DIR}/frontend.pid"
    }
    sleep 2
fi

# ------------------------------------------------------------------------------
# 4. Obrir al navegador
# ------------------------------------------------------------------------------
start http://localhost:4300/inici 2>/dev/null || true

echo "=========================================================="
echo "           DESPLEGAMENT COMPLETAT AMB ÈXIT!"
echo "=========================================================="
echo " - Base de dades: PostgreSQL a localhost:5432 (BD: jjaux_db)"
echo " - API Backend:   http://localhost:8080/api"
echo " - Aplicació Web: http://localhost:4300/inici"
echo " - Usuari Admin:  admin@admin.es / adminadmin"
echo "=========================================================="
echo "Per aturar tots els serveis: ./casa_aturar.sh"
echo "=========================================================="
