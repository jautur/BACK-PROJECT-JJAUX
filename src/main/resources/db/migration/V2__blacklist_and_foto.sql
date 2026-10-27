-- ==============================================================================
-- V2__blacklist_and_foto.sql - Columnes addicionals d'usuaris i taula de llista negra
-- ==============================================================================

ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS verificat BOOLEAN DEFAULT FALSE;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS sollicitud_verificacio BOOLEAN DEFAULT FALSE;
ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto TEXT;

CREATE TABLE IF NOT EXISTS blacklist_telefons (
    id VARCHAR(100) PRIMARY KEY,
    telefon VARCHAR(50) UNIQUE NOT NULL,
    motiu TEXT,
    bloquejat_per VARCHAR(100) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

