#!/usr/bin/env node

/**
** Script per a consultar la base de dades PostgreSQL (jjaux_db)
**
** Ús:
**   node consultar_db.js                       -> Mostra totes les taules i recompte de registres
**   node consultar_db.js usuaris               -> Mostra el contingut de la taula usuaris
**   node consultar_db.js "SELECT * FROM ... "  -> Executa qualsevol consulta SQL personalitzada
**/

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

async function run() {
  let client = new Client(config);
  try {
    await client.connect();
  } catch (err) {
    if (config.password === '' && (err.code === '28P01' || (err.message && err.message.toLowerCase().includes('password')))) {
      client = new Client({ ...config, password: 'postgres' });
      try {
        await client.connect();
      } catch (retryErr) {
        console.error('❌ Error connectant amb PostgreSQL:', retryErr.message);
        console.error('Assegura\'t que el servei PostgreSQL està en marxa al port 5432.');
        process.exit(1);
      }
    } else {
      console.error('❌ Error connectant amb PostgreSQL:', err.message);
      console.error('Assegura\'t que el servei PostgreSQL està en marxa al port 5432.');
      process.exit(1);
    }
  }

  const arg = process.argv.slice(2).join(' ').trim();

  try {
    if (!arg) {
      console.log('\n📊 === ESTAT DE LA BASE DE DADES: jjaux_db ===\n');
      console.log('📌 Paràmetres de connexió:');
      console.log('   - Host:     localhost (127.0.0.1)');
      console.log('   - Port:     5432');
      console.log('   - Usuari:   postgres');
      console.log('   - Password: (en blanc)');
      console.log('   - BD:       jjaux_db\n');

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
      console.log('💡 Exemples d\'ús:');
      console.log('   node consultar_db.js usuaris');
      console.log('   node consultar_db.js tasques');
      console.log('   node consultar_db.js ofertes');
      console.log('   node consultar_db.js resenyes');
      console.log('   node consultar_db.js "SELECT id, nom, email, rol, telefon FROM usuaris"');
      console.log('\n');
      return;
    }

    let sql = arg;
    const knownTables = ['usuaris', 'tasques', 'ofertes', 'propostes', 'avisos', 'resenyes', 'pressupostos', 'blacklist_telefons', 'publicacions', 'interessos'];
    if (knownTables.includes(arg.toLowerCase())) {
      sql = `SELECT * FROM ${arg.toLowerCase()} LIMIT 50`;
    }

    console.log(`\n🔍 Executant: ${sql}\n`);
    const res = await client.query(sql);

    if (res.rows && res.rows.length > 0) {
      console.table(res.rows);
      console.log(`Total: ${res.rows.length} fila/es retornades.\n`);
    } else {
      console.log('ℹ️ La consulta s\'ha executat correctament però no hi ha registres a mostrar.\n');
    }
  } catch (err) {
    console.error('❌ Error executant la consulta SQL:', err.message);
  } finally {
    try { await client.end(); } catch(e) {}
  }
}

run().then(() => process.exit(0)).catch((err) => {
  console.error(err);
  process.exit(1);
});

