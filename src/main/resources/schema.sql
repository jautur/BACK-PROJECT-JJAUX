-- ==============================================================================
-- Esquema DDL per a la Base de Dades jjaux_db (PostgreSQL)
-- Plataforma AVISA / JJAUX
-- ==============================================================================

CREATE TABLE IF NOT EXISTS usuaris (
    id VARCHAR(100) PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    telefon VARCHAR(50),
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasques (
    id VARCHAR(100) PRIMARY KEY,
    titol VARCHAR(255) NOT NULL,
    descripcio TEXT,
    categoria VARCHAR(100),
    client_nom VARCHAR(150),
    budget VARCHAR(50),
    estat VARCHAR(50) DEFAULT 'Oberta',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ofertes (
    id VARCHAR(100) PRIMARY KEY,
    tasca_id VARCHAR(100) REFERENCES tasques(id) ON DELETE CASCADE,
    professional VARCHAR(150) NOT NULL,
    price VARCHAR(50),
    note TEXT,
    estat VARCHAR(50) DEFAULT 'Pendent',
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
    treballador_id VARCHAR(100),
    tasca_id VARCHAR(100),
    puntuacio INTEGER NOT NULL,
    descripcio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

