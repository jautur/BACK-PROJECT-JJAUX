#!/usr/bin/env bash

# ==============================================================================
# Script de desplegament per a MOBIL / WI-FI LOCAL - JJAUX (Bash)
# Permet connectar qualsevol smartphone o tauleta a la mateixa xarxa Wi-Fi
# ==============================================================================

set -e

export PATH="/c/Program Files/nodejs:/mnt/c/Program Files/nodejs:/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin:/mnt/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin:/c/Program Files/PostgreSQL/18/bin:/mnt/c/Program Files/PostgreSQL/18/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" ]; then
    BACK_DIR="${SCRIPT_DIR}"
    FRONT_DIR="$(cd "${SCRIPT_DIR}/../FRONT-PROJECT-JJAUX" && pwd)"
elif [ -d "${SCRIPT_DIR}/BACK-PROJECT-JJAUX" ]; then
    BACK_DIR="$(cd "${SCRIPT_DIR}/BACK-PROJECT-JJAUX" && pwd)"
    FRONT_DIR="$(cd "${SCRIPT_DIR}/FRONT-PROJECT-JJAUX" && pwd)"
else
    FRONT_DIR="/c/Users/User/Desktop/FRONT-PROJECT-JJAUX"
    BACK_DIR="/c/Users/User/Desktop/BACK-PROJECT-JJAUX"
fi

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
echo "      INICIANT DESPLEGAMENT MOBIL PER WI-FI (CASA)"
echo "=========================================================="

free_port() {
    local port="$1"
    if command -v netstat >/dev/null 2>&1; then
        local pids=$(netstat -ano 2>/dev/null | grep ":${port} " | awk '{print $5}' | sort -u | grep -v '^0$' || true)
        for p in $pids; do
            taskkill //F //PID "$p" >/dev/null 2>&1 || true
        done
    fi
}

# 1. Base de dades PostgreSQL
echo -e "\n[1/4] Comprovant PostgreSQL..."
if node -e "const net = require('net'); const s = net.connect(5432, '127.0.0.1', () => { s.destroy(); process.exit(0); }); s.on('error', () => process.exit(1));" 2>/dev/null; then
    echo "  -> PostgreSQL ja està en marxa al port 5432."
else
    echo "  -> Engegant servei PostgreSQL..."
    net start postgresql-x64-18 2>/dev/null || true
    sleep 2
fi

# Assegurar admin
if [ -f "${FRONT_DIR}/assegurar_admin.js" ]; then
    node "${FRONT_DIR}/assegurar_admin.js" || true
elif [ -f "${BACK_DIR}/assegurar_admin.js" ]; then
    node "${BACK_DIR}/assegurar_admin.js" || true
fi

# 2. Backend Spring Boot
echo -e "\n[2/4] Comprovant Backend Spring Boot..."
JAR_FILE="${BACK_DIR}/target/jjaux-back-0.0.1-SNAPSHOT.jar"
WIN_JAR_FILE="${BACK_WIN}\\target\\jjaux-back-0.0.1-SNAPSHOT.jar"

if [ ! -f "${JAR_FILE}" ]; then
    echo "  -> Compilant el backend amb Maven..."
    cd "${BACK_DIR}"
    cmd.exe /c "mvnw.cmd package -DskipTests" || ./mvnw package -DskipTests
fi

if curl -s -o /dev/null http://localhost:8080/api/dashboard 2>/dev/null; then
    echo "  -> Backend ja està responent a http://localhost:8080"
else
    echo "  -> Engegant Spring Boot en segon pla..."
    free_port 8080
    cd "${BACK_DIR}"
    export SPRING_DATASOURCE_PASSWORD="postgres"

    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "\$p = Start-Process -FilePath 'java' -ArgumentList '-jar', '${WIN_JAR_FILE}' -WorkingDirectory '${BACK_WIN}' -RedirectStandardOutput '${BACK_WIN}\\backend.log' -RedirectStandardError '${BACK_WIN}\\backend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '${BACK_WIN}\\backend.pid' -Value \$p.Id" 2>/dev/null || true

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

# 3. Frontend Angular
echo -e "\n[3/4] Comprovant Frontend Angular (Port 4300)..."
if curl -s -o /dev/null http://localhost:4300 2>/dev/null; then
    echo "  -> Frontend ja està servint a 0.0.0.0:4300"
else
    if [ ! -d "${FRONT_DIR}/dist/front-project-jjaux-1/browser" ]; then
        echo "  -> Compilant aplicació Angular..."
        cd "${FRONT_DIR}"
        cmd.exe /c "npm run build" || npm run build
    fi

    echo "  -> Engegant servidor web del Frontend (port 4300 per a xarxa local)..."
    free_port 4300
    cd "${FRONT_DIR}"

    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "\$p = Start-Process -FilePath 'node' -ArgumentList 'serve.js' -WorkingDirectory '${FRONT_WIN}' -RedirectStandardOutput '${FRONT_WIN}\\frontend.log' -RedirectStandardError '${FRONT_WIN}\\frontend_err.log' -WindowStyle Minimized -PassThru; Set-Content -Path '${FRONT_WIN}\\frontend.pid' -Value \$p.Id" 2>/dev/null || true
    sleep 2
fi

# 4. Assegurar regla Firewall per al port 4300
netsh.exe advfirewall firewall add rule name="AVISA-Frontend-4300" dir=in action=allow protocol=TCP localport=4300 profile=any >/dev/null 2>&1 || true

# 5. Generar codi QR i obrir guia de connexió
echo -e "\n[4/4] Generant codi QR per a dispositius mòbils..."
if [ -f "${FRONT_DIR}/mostrar_qr_mobil.js" ]; then
    node "${FRONT_DIR}/mostrar_qr_mobil.js"
elif [ -f "${BACK_DIR}/mostrar_qr_mobil.js" ]; then
    node "${BACK_DIR}/mostrar_qr_mobil.js"
fi

echo ""
echo "=========================================================="
echo " 📲 Servidors engegats i actius en segon pla!"
echo " Escaneja el codi QR amb la càmera del mòbil."
echo " Per aturar-los en qualsevol moment: ./casa_aturar.sh"
echo "=========================================================="
read -rp " Prem [ENTER] quan hagis escanejat el codi QR per alliberar la terminal..."

