#!/usr/bin/env bash

# ==============================================================================
# Script de desplegament ràpid en local per al projecte JJAUX
# Inicia:
#   1. PostgreSQL (Port 5432 / Base de dades jjaux_db)
#   2. Backend Spring Boot (Port 8080)
#   3. Frontend Angular (Port 4300)
# ==============================================================================

set -e

PROJECT_DIR="/home/jautursis/Escriptori"
FRONT_DIR="${PROJECT_DIR}/FRONT-PROJECT-JJAUX"
BACK_DIR="${PROJECT_DIR}/BACK-PROJECT-JJAUX"
PG_DIR="/home/jautursis/.gemini/antigravity/brain/ac12e9c2-506a-4129-bb08-8c851cfd0667/scratch/pg"
MAVEN_BIN="/home/jautursis/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin/mvn"
PG_BIN="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/bin"
PG_LIB="${PG_DIR}/node_modules/@embedded-postgres/linux-x64/native/lib"
PG_DATA="${PG_DIR}/data"

echo "=========================================================="
echo "          INICIANT DESPLEGAMENT LOCAL JJAUX"
echo "=========================================================="

# ------------------------------------------------------------------------------
# 1. Base de dades PostgreSQL (Port 5432)
# ------------------------------------------------------------------------------
echo -e "\n[1/3] Comprovant PostgreSQL..."
if nc -z 127.0.0.1 5432 2>/dev/null || node -e "const net = require('net'); const s = net.connect(5432, '127.0.0.1', () => { process.exit(0); }); s.on('error', () => process.exit(1));" 2>/dev/null; then
    echo "  -> PostgreSQL ja està en marxa al port 5432."
else
    echo "  -> Engegant servidor PostgreSQL..."
    LD_LIBRARY_PATH="${PG_LIB}" "${PG_BIN}/pg_ctl" -D "${PG_DATA}" -l "${PG_DIR}/postgres.log" -o "-p 5432" start || true
    sleep 2
fi

# Inicialització de taules i dades si cal
echo "  -> Verificant esquema i dades a jjaux_db..."
cd "${PG_DIR}" && node init_db.js >/dev/null 2>&1 || true

# Assegurar usuari administrador (admin@admin.es / adminadmin)
echo "  -> Verificant usuari administrador (admin@admin.es)..."
if [ -f "${FRONT_DIR}/assegurar_admin.js" ]; then
    node "${FRONT_DIR}/assegurar_admin.js" || true
elif [ -f "${BACK_DIR}/assegurar_admin.js" ]; then
    node "${BACK_DIR}/assegurar_admin.js" || true
else
    node -e "
    let pg;
    try { pg = require('pg'); } catch(e) {
      try { pg = require('${FRONT_DIR}/node_modules/pg'); } catch(e2) {
        try { pg = require('${PG_DIR}/node_modules/pg'); } catch(e3) {}
      }
    }
    if (!pg) process.exit(0);
    const { Client } = pg;
    (async () => {
      let client = new Client({ host: 'localhost', port: 5432, user: 'postgres', password: process.env.PGPASSWORD !== undefined ? process.env.PGPASSWORD : '', database: 'jjaux_db' });
      try {
        await client.connect();
      } catch(err) {
        try {
          client = new Client({ host: 'localhost', port: 5432, user: 'postgres', password: 'postgres', database: 'jjaux_db' });
          await client.connect();
        } catch(err2) {
          process.exit(0);
        }
      }
      try {
        await client.query(\`
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
        const chk = await client.query(\"SELECT id FROM usuaris WHERE LOWER(email) = 'admin@admin.es'\");
        if (chk.rows.length === 0) {
          const chkAdmin = await client.query(\"SELECT id FROM usuaris WHERE id = 'u-admin'\");
          if (chkAdmin.rows.length > 0) {
            await client.query(\"UPDATE usuaris SET email = 'admin@admin.es', password = 'adminadmin', rol = 'admin' WHERE id = 'u-admin'\");
            console.log('  -> Usuari administrador u-admin actualitzat a admin@admin.es.');
          } else {
            await client.query(\"INSERT INTO usuaris (id, nom, email, password, rol, bio) VALUES ('u-admin', 'Administrador General', 'admin@admin.es', 'adminadmin', 'admin', 'Administrador de la plataforma AVISA')\");
            console.log('  -> Usuari administrador creat: admin@admin.es (password: adminadmin).');
          }
        } else {
          await client.query(\"UPDATE usuaris SET password = 'adminadmin', rol = 'admin' WHERE LOWER(email) = 'admin@admin.es'\");
          console.log('  -> Usuari administrador admin@admin.es verificat (password: adminadmin).');
        }
      } catch(e) {} finally { await client.end().catch(()=>{}); }
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
        "${MAVEN_BIN}" clean package -DskipTests
    fi

    echo "  -> Engegant Spring Boot en segon pla..."
    nohup java -jar "${JAR_FILE}" > "${BACK_DIR}/backend.log" 2>&1 &
    BACKEND_PID=$!
    disown $BACKEND_PID 2>/dev/null || true
    echo $BACKEND_PID > "${BACK_DIR}/backend.pid"

    echo -n "  -> Esperant que el backend estigui llest..."
    for i in {1..30}; do
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
        cd "${FRONT_DIR}" && npm run build
    fi

    echo "  -> Engegant servidor web del Frontend..."
    cd "${FRONT_DIR}"
    nohup node serve.js > "${FRONT_DIR}/frontend.log" 2>&1 &
    FRONT_PID=$!
    disown $FRONT_PID 2>/dev/null || true
    echo $FRONT_PID > "${FRONT_DIR}/frontend.pid"
    sleep 1
fi

echo "=========================================================="
echo "           DESPLEGAMENT COMPLETAT AMB ÈXIT!"
echo "=========================================================="
echo " - Base de dades: PostgreSQL a localhost:5432 (BD: jjaux_db)"
echo " - API Backend:   http://localhost:8080/api"
echo " - Aplicació Web: http://localhost:4300"
echo " - Usuari Admin:  admin@admin.es / adminadmin"
echo "=========================================================="
echo "Per aturar tots els serveis: ./aturar.sh"
echo "=========================================================="

