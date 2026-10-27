-- ==============================================================================
-- Script SQL: Buidar la Base de Dades jjaux_db (PostgreSQL)
-- Plataforma AVISA / JJAUX
-- Buida tasques, incidències, ofertes, avisos, publicacions, interessos, pressupostos,
-- resenyes i llista negra de telèfons respectant totes les claus foranes (Foreign Keys)
-- Manté únicament els 3 usuaris base: admin, cli i pro
-- ==============================================================================

-- 1. Assegurar columnes necessàries per a les entitats JPA i funcionalitats noves
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS percentil INT DEFAULT 3 CHECK (percentil >= 0 AND percentil <= 100);
UPDATE usuaris SET percentil = 3 WHERE percentil IS NULL;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS verificat BOOLEAN DEFAULT FALSE;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS sollicitud_verificacio BOOLEAN DEFAULT FALSE;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto TEXT;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto_perfil TEXT;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS logo TEXT;
ALTER TABLE avisos ADD COLUMN IF NOT EXISTS enllac VARCHAR(255);
ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS categoria VARCHAR(100);
ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS incidencia_id VARCHAR(100);
ALTER TABLE resenyes ADD COLUMN IF NOT EXISTS comentari TEXT;
ALTER TABLE resenyes ADD COLUMN IF NOT EXISTS professional_nom VARCHAR(150);

CREATE TABLE IF NOT EXISTS blacklist_telefons (
    id VARCHAR(100) PRIMARY KEY,
    telefon VARCHAR(50) UNIQUE NOT NULL,
    motiu TEXT,
    bloquejat_per VARCHAR(100) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Buidat segur de taules respectant claus foranes i dependències d'entitat
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

-- 3. Inserir els 3 usuaris de base
-- Admin (admin@admin.es / adminadmin)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio, percentil, verificat)
VALUES (
    'u-admin',
    'Administrador General',
    'admin@admin.es',
    'adminadmin',
    'admin',
    NULL,
    'Administrador de la plataforma AVISA',
    3,
    TRUE
);

-- Client de proves (cli / clicli)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio, percentil)
VALUES (
    'u-cli',
    'cli',
    'cli',
    'clicli',
    'client',
    '600111222',
    'Compte client de proves per a creació d''incidències.',
    3
);

-- Professional de proves (pro / propro)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio, percentil, verificat)
VALUES (
    'u-pro',
    'pro',
    'pro',
    'propro',
    'professional',
    '000444555',
    'Compte professional de proves per a pressupostos i ofertes.',
    3,
    TRUE
);
