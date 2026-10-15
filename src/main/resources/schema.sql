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

-- Incidències creades exclusivament per clients (demandes de serveis a la llar/negoci)
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

-- Ofertes i pressupostos proposats exclusivament per professionals
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

-- Tasques actives on convergeixen client i professional obligatoris
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
