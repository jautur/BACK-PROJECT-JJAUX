-- ==============================================================================
-- Script SQL: Buidar la Base de Dades jjaux_db (PostgreSQL)
-- Plataforma AVISA / JJAUX
-- Buida totes les taules de l'esquema de V1__esquema_inicial.sql.
-- Manté únicament els 3 usuaris base: admin, cli i pro (els mateixos de DevUsersSeeder).
-- L'esquema el crea Flyway en arrancar el backend; aquest script no el modifica.
-- ==============================================================================

BEGIN;

TRUNCATE TABLE
    sollicituds_cancellacio,
    avisos,
    tasques,
    pressupostos,
    publicacions,
    blacklist_telefons,
    usuaris
CASCADE;

INSERT INTO usuaris (nom, email, password, rol, telefon, verificat) VALUES
    ('Administrador General', 'admin@admin.es', 'adminadmin', 'ADMIN', '960000000', TRUE),
    ('cli', 'cli', 'clicli', 'CLIENT', '600111222', FALSE),
    ('pro', 'pro', 'propro', 'PROFESSIONAL', '600333444', TRUE);

COMMIT;
