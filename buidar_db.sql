-- ==============================================================================
-- Script SQL: Buidar la Base de Dades jjaux_db (PostgreSQL)
-- Plataforma AVISA / JJAUX
-- Buida tasques, incidències, ofertes i avisos
-- Manté únicament els 3 usuaris base: admin, cli i pro
-- ==============================================================================

-- 1. Buidar activitat
DELETE FROM tasques;
DELETE FROM incidencies;
DELETE FROM ofertes;
DELETE FROM avisos;
DO $$ 
BEGIN 
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'resenyes') THEN 
        DELETE FROM resenyes; 
    END IF; 
END $$;

-- 2. Netejar usuaris
DELETE FROM usuaris;

-- 3. Inserir els 3 usuaris de base
-- Admin (admin@admin.es / adminadmin)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio)
VALUES (
    'u-admin',
    'Administrador General',
    'admin@admin.es',
    'adminadmin',
    'admin',
    NULL,
    'Administrador de la plataforma AVISA'
);

-- Client de proves (cli / clicli)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio)
VALUES (
    'u-cli',
    'cli',
    'cli',
    'clicli',
    'client',
    '600111222',
    'Compte client de proves per a creació d''incidències.'
);

-- Professional de proves (pro / propro)
INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio)
VALUES (
    'u-pro',
    'pro',
    'pro',
    'propro',
    'professional',
    '000444555',
    'Compte professional de proves per a pressupostos i ofertes.'
);

