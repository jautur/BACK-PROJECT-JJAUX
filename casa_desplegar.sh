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
export PATH="$PATH:/c/Program Files/nodejs:/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin:/c/Program Files/PostgreSQL/18/bin"

# Directoris del projecte a CASA
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
    FRONT_DIR="/c/Users/User/Desktop/FRONT-PROJECT-JJAUX"
    BACK_DIR="/c/Users/User/Desktop/BACK-PROJECT-JJAUX"
fi

echo "=========================================================="
echo "          INICIANT DESPLEGAMENT LOCAL JJAUX (CASA)"
echo "=========================================================="

# ------------------------------------------------------------------------------
# 1. Base de dades PostgreSQL (Port 5432)
# ------------------------------------------------------------------------------
echo -e "\n[1/3] Comprovant PostgreSQL..."
if nc -z 127.0.0.1 5432 2>/dev/null || node -e "const net = require('net'); const s = net.connect(5432, '127.0.0.1', () => { process.exit(0); }); s.on('error', () => process.exit(1));" 2>/dev/null; then
    echo "  -> PostgreSQL ja està en marxa al port 5432."
else
    echo "  -> Engegant servei PostgreSQL..."
    net start postgresql-x64-18 2>/dev/null || "/c/Program Files/PostgreSQL/18/bin/pg_ctl.exe" start -D "/c/Program Files/PostgreSQL/18/data" 2>/dev/null || true
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
    // ignorar si ja existeix
  } finally {
    await c.end();
  }
})();
" 2>/dev/null || true

# Assegurar usuari administrador (admin@admin.es / adminadmin)
echo "  -> Verificant usuari administrador (admin@admin.es)..."
if [ -f "${FRONT_DIR}/assegurar_admin.js" ]; then
    node "${FRONT_DIR}/assegurar_admin.js" || true
elif [ -f "${BACK_DIR}/assegurar_admin.js" ]; then
    node "${BACK_DIR}/assegurar_admin.js" || true
else
    node -e "
    let pg;
    try { pg = require('pg'); } catch(e) { try { pg = require('${FRONT_DIR}/node_modules/pg'); } catch(e2){} }
    if (!pg) process.exit(0);
    const { Client } = pg;
    (async () => {
      const c = new Client({ host: 'localhost', port: 5432, user: 'postgres', password: process.env.PGPASSWORD || 'postgres', database: 'jjaux_db' });
      try {
        await c.connect();
        await c.query(\`
          CREATE TABLE IF NOT EXISTS usuaris (
            id VARCHAR(100) PRIMARY KEY,
            nom VARCHAR(150) NOT NULL,
            email VARCHAR(150) UNIQUE NOT NULL,
            password VARCHAR(255) NOT NULL,
            rol VARCHAR(50) NOT NULL,
            telefon VARCHAR(50),
            bio TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
        \`);
        const chk = await c.query(\"SELECT id FROM usuaris WHERE LOWER(email) = 'admin@admin.es'\");
        if (chk.rows.length === 0) {
          const chkAdmin = await c.query(\"SELECT id FROM usuaris WHERE id = 'u-admin'\");
          if (chkAdmin.rows.length > 0) {
            await c.query(\"UPDATE usuaris SET email = 'admin@admin.es', password = 'adminadmin', rol = 'admin' WHERE id = 'u-admin'\");
            console.log('  -> Usuari administrador u-admin actualitzat a admin@admin.es.');
          } else {
            await c.query(\"INSERT INTO usuaris (id, nom, email, password, rol, bio) VALUES ('u-admin', 'Administrador General', 'admin@admin.es', 'adminadmin', 'admin', 'Administrador de la plataforma AVISA')\");
            console.log('  -> Usuari administrador creat: admin@admin.es (password: adminadmin).');
          }
        } else {
          await c.query(\"UPDATE usuaris SET password = 'adminadmin', rol = 'admin' WHERE LOWER(email) = 'admin@admin.es'\");
          console.log('  -> Usuari administrador admin@admin.es verificat (password: adminadmin).');
        }
      } catch(e) {} finally { await c.end().catch(()=>{}); }
    })();
    " 2>/dev/null || true
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
        ./mvnw package -DskipTests
    fi

    echo "  -> Engegant Spring Boot en segon pla..."
    cd "${BACK_DIR}"
    export SPRING_DATASOURCE_PASSWORD="postgres"
    nohup java -jar "${JAR_FILE}" > "${BACK_DIR}/backend.log" 2>&1 &
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
echo -e "\n[3/3] Comprovant Frontend Angular..."
if nc -z 127.0.0.1 4300 2>/dev/null || curl -s -o /dev/null http://localhost:4300 2>/dev/null; then
    echo "  -> Frontend ja està servint a http://localhost:4300"
else
    if [ ! -d "${FRONT_DIR}/dist/front-project-jjaux-1/browser" ]; then
        echo "  -> Compilant aplicació Angular..."
        cd "${FRONT_DIR}"
        npm run build || npm.cmd run build
    fi

    echo "  -> Engegant servidor web del Frontend..."
    cd "${FRONT_DIR}"
    nohup node serve.js > "${FRONT_DIR}/frontend.log" 2>&1 &
    FRONT_PID=$!
    disown $FRONT_PID 2>/dev/null || true
    echo $FRONT_PID > "${FRONT_DIR}/frontend.pid"
    sleep 1
fi

# ------------------------------------------------------------------------------
# 4. Obrir al navegador
# ------------------------------------------------------------------------------
echo -e "\n  -> Obrint aplicació al navegador..."
if command -v cmd.exe >/dev/null 2>&1; then
    cmd.exe /c start http://localhost:4300 2>/dev/null || true
elif command -v start >/dev/null 2>&1; then
    start http://localhost:4300 2>/dev/null || true
elif command -v explorer.exe >/dev/null 2>&1; then
    explorer.exe "http://localhost:4300" 2>/dev/null || true
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open http://localhost:4300 2>/dev/null || true
fi

echo "=========================================================="
echo "           DESPLEGAMENT COMPLETAT AMB ÈXIT!"
echo "=========================================================="
echo " - Base de dades: PostgreSQL a localhost:5432 (BD: jjaux_db)"
echo " - API Backend:   http://localhost:8080/api"
echo " - Aplicació Web: http://localhost:4300"
echo " - Usuari Admin:  admin@admin.es / adminadmin"
echo "=========================================================="
echo "Per aturar tots els serveis: ./casa_aturar.sh"
echo "=========================================================="

