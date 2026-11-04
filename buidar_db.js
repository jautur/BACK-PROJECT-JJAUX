#!/usr/bin/env node

/**
 * ==============================================================================
 * Script per a buidar la base de dades PostgreSQL (jjaux_db)
 * Manté únicament els 3 usuaris base de la plataforma:
 *   1. admin (admin@admin.es / adminadmin)
 *   2. cli   (cli / clicli)
 *   3. pro   (pro / propro)
 *
 * Suporta entorns de CASA (password 'postgres') i de CLASE (password buida o 'postgres').
 * Ús:
 *   node buidar_db.js          (auto-detecció de contrasenya)
 *   node buidar_db.js casa     (per defecte password 'postgres')
 *   node buidar_db.js clase    (per defecte password buida '')
 * ==============================================================================
 */

let pg;
try {
  pg = require('pg');
} catch (e) {
  try {
    const path = require('path');
    pg = require(path.join(__dirname, '..', 'FRONT-PROJECT-JJAUX', 'node_modules', 'pg'));
  } catch (e2) {
    try {
      pg = require('/home/jautursis/.gemini/antigravity/brain/ac12e9c2-506a-4129-bb08-8c851cfd0667/scratch/pg/node_modules/pg');
    } catch (e3) {
      console.error('❌ No s\'ha trobat el mòdul "pg".');
      console.error('Assegura\'t de tenir les dependències instal·lades a FRONT-PROJECT-JJAUX.');
      process.exit(1);
    }
  }
}
const { Client } = pg;
const fs = require('fs');
const path = require('path');

const mode = (process.argv[2] || '').toLowerCase();
const isCasa = mode === 'casa';
const isClase = mode === 'clase';

const baseConfig = {
  host: process.env.PGHOST || 'localhost',
  port: parseInt(process.env.PGPORT || '5432', 10),
  user: process.env.PGUSER || 'postgres',
  database: process.env.PGDATABASE || 'jjaux_db'
};

async function tryConnect(password) {
  const client = new Client({ ...baseConfig, password });
  try {
    await client.connect();
    return client;
  } catch (err) {
    try { await client.end(); } catch (e) {}
    throw err;
  }
}

async function getConnectedClient() {
  const envPassword = process.env.PGPASSWORD || process.env.DB_PASSWORD;
  
  if (envPassword !== undefined) {
    try {
      return await tryConnect(envPassword);
    } catch (e) {}
  }

  if (isCasa) {
    // A casa habitualment és 'postgres'
    try {
      return await tryConnect('postgres');
    } catch (e) {
      try {
        return await tryConnect('');
      } catch (e2) {
        throw e;
      }
    }
  }

  if (isClase) {
    // A classe habitualment és sense password ''
    try {
      return await tryConnect('');
    } catch (e) {
      try {
        return await tryConnect('postgres');
      } catch (e2) {
        throw e;
      }
    }
  }

  // Auto-detecció
  try {
    return await tryConnect(process.env.PGPASSWORD || 'postgres');
  } catch (err) {
    try {
      return await tryConnect('');
    } catch (err2) {
      console.error('❌ Error connectant a PostgreSQL amb usuari "postgres":', err.message);
      console.error('Comprova que el servei de PostgreSQL estigui actiu al port 5432.');
      process.exit(1);
    }
  }
}

async function main() {
  console.log('\n==========================================================');
  console.log('       BUIDAT DE BASE DE DADES JJAUX (AVISA)');
  if (isCasa) console.log('       Mode: CASA (Password: postgres)');
  else if (isClase) console.log('       Mode: CLASE (Password buida / postgres)');
  else console.log('       Mode: AUTO');
  console.log('==========================================================\n');

  let client;
  try {
    client = await getConnectedClient();
    console.log('✅ Connectat amb èxit a PostgreSQL jjaux_db (localhost:5432).');
  } catch (e) {
    console.error('❌ No s\'ha pogut connectar a PostgreSQL:', e.message);
    process.exit(1);
  }

  try {
    // 1. L'esquema és de Flyway: sense esquema no hi ha res a buidar
    const esquema = await client.query("SELECT to_regclass('public.usuaris') AS taula");
    if (!esquema.rows[0].taula) {
      console.log('ℹ️  Encara no hi ha esquema: arranca el backend perquè Flyway el cree.');
      return;
    }

    // 2 i 3. Buidar les taules i deixar els 3 usuaris base, en una sola transacció
    console.log('🧹 Buidant les taules i deixant exclusivament admin, cli i pro...');
    await client.query(fs.readFileSync(path.join(__dirname, 'buidar_db.sql'), 'utf8'));

    console.log('✅ Base de dades buidada i reiniciada correctament!\n');

    // 4. Mostrar estat final
    console.log('📊 === RECOMPTE FINAL DE REGISTRES A JJAUX_DB ===');
    const tablesRes = await client.query(`
      SELECT tablename 
      FROM pg_tables 
      WHERE schemaname = 'public' 
      ORDER BY tablename ASC
    `);

    const stats = [];
    for (const row of tablesRes.rows) {
      const countRes = await client.query(`SELECT COUNT(*) as total FROM "${row.tablename}"`);
      stats.push({
        Taula: row.tablename,
        Registres: parseInt(countRes.rows[0].total, 10)
      });
    }
    console.table(stats);

    console.log('🔑 === USUARIS DISPONIBLES PER A PROVES ===');
    const usersRes = await client.query('SELECT id, nom, email, password, rol, telefon, percentil FROM usuaris ORDER BY id');
    console.table(usersRes.rows);

    console.log('✨ Tot a punt per testejar des de zero tant a casa com a classe!');
    console.log('==========================================================\n');
  } catch (err) {
    console.error('❌ Error durant el buidat de la base de dades:', err.message);
    process.exit(1);
  } finally {
    try { await client.end(); } catch(e) {}
  }
}

main().then(() => process.exit(0)).catch((err) => {
  console.error(err);
  process.exit(1);
});
