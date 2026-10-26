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
    // 1. Assegurar que les taules existeixen
    await client.query(`
      CREATE TABLE IF NOT EXISTS usuaris (
        id VARCHAR(100) PRIMARY KEY,
        nom VARCHAR(150) NOT NULL,
        email VARCHAR(150) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        rol VARCHAR(50) NOT NULL,
        telefon VARCHAR(50),
        bio TEXT,
        percentil INT DEFAULT 3 CHECK (percentil >= 0 AND percentil <= 100),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS incidencies (
        id VARCHAR(100) PRIMARY KEY,
        client_id VARCHAR(100),
        client_nom VARCHAR(150) NOT NULL,
        titol VARCHAR(255) NOT NULL,
        descripcio TEXT,
        categoria VARCHAR(100),
        budget VARCHAR(50),
        estat VARCHAR(50) DEFAULT 'Oberta',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS ofertes (
        id VARCHAR(100) PRIMARY KEY,
        tasca_id VARCHAR(100),
        incidencia_id VARCHAR(100),
        professional VARCHAR(150) NOT NULL,
        price VARCHAR(50),
        note TEXT,
        categoria VARCHAR(100),
        estat VARCHAR(50) DEFAULT 'Pendent',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS tasques (
        id VARCHAR(100) PRIMARY KEY,
        titol VARCHAR(255) NOT NULL,
        descripcio TEXT,
        categoria VARCHAR(100),
        client_id VARCHAR(100),
        client_nom VARCHAR(150),
        professional_id VARCHAR(100),
        professional_nom VARCHAR(150),
        origen_id VARCHAR(100),
        budget VARCHAR(50),
        estat VARCHAR(50) DEFAULT 'En curs',
        tipus VARCHAR(50) DEFAULT 'tasca',
        valoracio INTEGER,
        opinio TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS avisos (
        id VARCHAR(100) PRIMARY KEY,
        usuari_id VARCHAR(100),
        missatge TEXT NOT NULL,
        llegit BOOLEAN DEFAULT FALSE,
        data DATE DEFAULT CURRENT_DATE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS resenyes (
        id VARCHAR(100) PRIMARY KEY,
        client_id VARCHAR(100),
        client_nom VARCHAR(150),
        treballador_id VARCHAR(100),
        treballador_nom VARCHAR(150),
        tasca_id VARCHAR(100),
        puntuacio INTEGER NOT NULL,
        descripcio TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS publicacions (
        id VARCHAR(100) PRIMARY KEY,
        usuari_id VARCHAR(100),
        usuari_nom VARCHAR(150) NOT NULL,
        tipus VARCHAR(50) NOT NULL,
        titol VARCHAR(255) NOT NULL,
        descripcio TEXT,
        categoria VARCHAR(100),
        preu_o_pressupost VARCHAR(50),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS interessos (
        id VARCHAR(100) PRIMARY KEY,
        publicacio_id VARCHAR(100),
        client_id VARCHAR(100),
        client_nom VARCHAR(150) NOT NULL,
        missatge TEXT,
        tasca_id VARCHAR(100),
        data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS pressupostos (
        id VARCHAR(100) PRIMARY KEY,
        incidencia_id VARCHAR(100),
        professional_id VARCHAR(100),
        professional_nom VARCHAR(150) NOT NULL,
        preu VARCHAR(50) NOT NULL,
        descripcio TEXT,
        duracio_estimada VARCHAR(100),
        data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS blacklist_telefons (
        id VARCHAR(100) PRIMARY KEY,
        telefon VARCHAR(50) UNIQUE NOT NULL,
        motiu TEXT,
        bloquejat_per VARCHAR(100) DEFAULT 'admin',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS percentil INT DEFAULT 3 CHECK (percentil >= 0 AND percentil <= 100);
      UPDATE usuaris SET percentil = 3 WHERE percentil IS NULL;
      ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS verificat BOOLEAN DEFAULT FALSE;
      ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS sollicitud_verificacio BOOLEAN DEFAULT FALSE;
      ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto TEXT;
      ALTER TABLE avisos ADD COLUMN IF NOT EXISTS enllac VARCHAR(255);
      ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS categoria VARCHAR(100);
      ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS incidencia_id VARCHAR(100);
      ALTER TABLE ofertes ALTER COLUMN tasca_id DROP NOT NULL;
      ALTER TABLE resenyes ADD COLUMN IF NOT EXISTS comentari TEXT;
      ALTER TABLE resenyes ADD COLUMN IF NOT EXISTS professional_nom VARCHAR(150);
      ALTER TABLE resenyes ALTER COLUMN descripcio DROP NOT NULL;
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_inici TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_finalitzacio TIMESTAMP;
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_valoracio TIMESTAMP;
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS interes_id VARCHAR(100);
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS pressupost_id VARCHAR(100);
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS preu_acordat VARCHAR(50);
      ALTER TABLE tasques ADD COLUMN IF NOT EXISTS publicacio_id VARCHAR(100);
    `);

    // 2. Buidar totes les taules d'activitat i usuaris respectant dependències i claus foranes (FK)
    console.log('🧹 Buidant taules d\'activitat (publicacions, interessos, pressupostos, tasques, incidències, ofertes, avisos, resenyes) respectant claus foranes...');
    try {
      await client.query(`
        TRUNCATE TABLE 
          blacklist_telefons,
          interessos, 
          pressupostos, 
          ofertes, 
          resenyes, 
          avisos, 
          tasques, 
          incidencies, 
          publicacions, 
          usuaris 
        RESTART IDENTITY CASCADE;
      `);
    } catch (truncateErr) {
      // Fallback si alguna taula no existís encara en aquest punt
      try { await client.query('DELETE FROM blacklist_telefons;'); } catch (e) {}
      try { await client.query('DELETE FROM interessos;'); } catch (e) {}
      try { await client.query('DELETE FROM pressupostos;'); } catch (e) {}
      try { await client.query('DELETE FROM ofertes;'); } catch (e) {}
      try { await client.query('DELETE FROM resenyes;'); } catch (e) {}
      try { await client.query('DELETE FROM avisos;'); } catch (e) {}
      try { await client.query('DELETE FROM tasques;'); } catch (e) {}
      try { await client.query('DELETE FROM incidencies;'); } catch (e) {}
      try { await client.query('DELETE FROM publicacions;'); } catch (e) {}
      try { await client.query('DELETE FROM usuaris;'); } catch (e) {}
    }

    // 3. Reiniciar als 3 usuaris base amb tots els camps complets (incloent percentil)
    console.log('👤 Reiniciant usuaris: deixant exclusivament admin, cli i pro...');
    await client.query(`
      INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio, percentil)
      VALUES 
        ('u-admin', 'Administrador General', 'admin@admin.es', 'adminadmin', 'admin', NULL, 'Administrador de la plataforma AVISA', 3),
        ('u-cli', 'cli', 'cli', 'clicli', 'client', '600111222', 'Compte client de proves per a creació d''incidències.', 3),
        ('u-pro', 'pro', 'pro', 'propro', 'professional', '000444555', 'Compte professional de proves per a pressupostos i ofertes.', 3);
    `);

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
