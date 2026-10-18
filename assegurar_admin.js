#!/usr/bin/env node

/**
 * Script per assegurar l'existència de l'usuari administrador principal
 * Credencials:
 *   - Email:    admin@admin.es
 *   - Password: adminadmin
 *   - Rol:      admin
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
      console.error('❌ Mòdul "pg" no trobat.');
      process.exit(1);
    }
  }
}
const { Client } = pg;

const config = {
  host: process.env.PGHOST || 'localhost',
  port: parseInt(process.env.PGPORT || '5432', 10),
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD !== undefined ? process.env.PGPASSWORD : (process.env.DB_PASSWORD || ''),
  database: process.env.PGDATABASE || 'jjaux_db'
};

async function connectClient() {
  let client = new Client(config);
  try {
    await client.connect();
    return client;
  } catch (err) {
    if (config.password === '' && (err.code === '28P01' || (err.message && err.message.toLowerCase().includes('password')))) {
      client = new Client({ ...config, password: 'postgres' });
      try {
        await client.connect();
        return client;
      } catch (retryErr) {
        return null;
      }
    }
    return null;
  }
}

async function assegurarAdmin() {
  const client = await connectClient();
  if (!client) {
    console.log('⚠️ No s\'ha pogut connectar a PostgreSQL directament. Spring Boot s\'encarregarà d\'inicialitzar l\'usuari.');
    return;
  }

  try {
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
      ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS percentil INT DEFAULT 3 CHECK (percentil >= 0 AND percentil <= 100);
      UPDATE usuaris SET percentil = 3 WHERE percentil IS NULL;
    `);

    const check = await client.query("SELECT id, nom, email, password, rol, percentil FROM usuaris WHERE LOWER(email) = 'admin@admin.es'");
    if (check.rows.length === 0) {
      const checkAdmin = await client.query("SELECT id FROM usuaris WHERE id = 'u-admin'");
      if (checkAdmin.rows.length > 0) {
        await client.query("UPDATE usuaris SET email = 'admin@admin.es', password = 'adminadmin', rol = 'admin', percentil = 3 WHERE id = 'u-admin'");
        console.log('✅ Usuari administrador u-admin actualitzat a: admin@admin.es (password: adminadmin)');
      } else {
        await client.query("INSERT INTO usuaris (id, nom, email, password, rol, bio, percentil) VALUES ('u-admin', 'Administrador General', 'admin@admin.es', 'adminadmin', 'admin', 'Administrador de la plataforma AVISA', 3)");
        console.log('✅ Usuari administrador creat: admin@admin.es (password: adminadmin)');
      }
    } else {
      await client.query("UPDATE usuaris SET password = 'adminadmin', rol = 'admin', percentil = 3 WHERE LOWER(email) = 'admin@admin.es'");
      console.log('✅ Usuari administrador admin@admin.es verificat i actualitzat (password: adminadmin)');
    }

    const resFinal = await client.query("SELECT id, nom, email, password, rol FROM usuaris WHERE LOWER(email) = 'admin@admin.es'");
    console.table(resFinal.rows);
  } catch (err) {
    console.error('❌ Error gestionant l\'usuari administrador:', err.message);
  } finally {
    await client.end().catch(() => {});
  }
}

assegurarAdmin();

