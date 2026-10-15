-- ==============================================================================
-- V1__initial_schema.sql - Migració inicial completa per a la plataforma AVISA
-- Gestionada oficialment per Flyway sobre PostgreSQL
-- ==============================================================================

-- 1. Taula d'Usuaris
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

-- 2. Taula de Publicacions unificades (Ofertes de servei i Incidències)
CREATE TABLE IF NOT EXISTS publicacions (
    id VARCHAR(100) PRIMARY KEY,
    usuari_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    usuari_nom VARCHAR(150) NOT NULL,
    tipus VARCHAR(50) NOT NULL,
    titol VARCHAR(255) NOT NULL,
    descripcio TEXT,
    categoria VARCHAR(100),
    preu_o_pressupost VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Taula d'Incidències
CREATE TABLE IF NOT EXISTS incidencies (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    client_nom VARCHAR(150) NOT NULL,
    titol VARCHAR(255) NOT NULL,
    descripcio TEXT,
    categoria VARCHAR(100),
    budget VARCHAR(50),
    estat VARCHAR(50) DEFAULT 'Oberta',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Taula de Tasques
CREATE TABLE IF NOT EXISTS tasques (
    id VARCHAR(100) PRIMARY KEY,
    titol VARCHAR(255) NOT NULL,
    descripcio TEXT,
    categoria VARCHAR(100),
    client_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    client_nom VARCHAR(150),
    professional_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    professional_nom VARCHAR(150),
    origen_id VARCHAR(100),
    budget VARCHAR(50),
    estat VARCHAR(50) DEFAULT 'En curs',
    tipus VARCHAR(50) DEFAULT 'tasca',
    valoracio INTEGER CHECK (valoracio IS NULL OR (valoracio >= 1 AND valoracio <= 5)),
    opinio TEXT,
    preu_acordat VARCHAR(50),
    data_inici TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_finalitzacio TIMESTAMP,
    data_valoracio TIMESTAMP,
    interes_id VARCHAR(100),
    pressupost_id VARCHAR(100),
    publicacio_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Taula d'Ofertes
CREATE TABLE IF NOT EXISTS ofertes (
    id VARCHAR(100) PRIMARY KEY,
    tasca_id VARCHAR(100) REFERENCES tasques(id) ON DELETE CASCADE,
    incidencia_id VARCHAR(100) REFERENCES incidencies(id) ON DELETE CASCADE,
    professional VARCHAR(150) NOT NULL,
    price VARCHAR(50) NOT NULL,
    note TEXT,
    categoria VARCHAR(100),
    estat VARCHAR(50) DEFAULT 'Pendent',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Taula de Pressupostos
CREATE TABLE IF NOT EXISTS pressupostos (
    id VARCHAR(100) PRIMARY KEY,
    incidencia_id VARCHAR(100) REFERENCES incidencies(id) ON DELETE CASCADE,
    professional_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    professional_nom VARCHAR(150) NOT NULL,
    preu VARCHAR(50) NOT NULL,
    descripcio TEXT,
    duracio_estimada VARCHAR(100),
    data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Taula d'Interessos
CREATE TABLE IF NOT EXISTS interessos (
    id VARCHAR(100) PRIMARY KEY,
    publicacio_id VARCHAR(100) REFERENCES publicacions(id) ON DELETE CASCADE,
    client_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE SET NULL,
    client_nom VARCHAR(150) NOT NULL,
    missatge TEXT,
    tasca_id VARCHAR(100) REFERENCES tasques(id) ON DELETE SET NULL,
    data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Taula de Ressenyes
CREATE TABLE IF NOT EXISTS resenyes (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    client_nom VARCHAR(150),
    treballador_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    treballador_nom VARCHAR(150),
    professional_nom VARCHAR(150),
    tasca_id VARCHAR(100) REFERENCES tasques(id) ON DELETE SET NULL,
    puntuacio INTEGER NOT NULL CHECK (puntuacio >= 1 AND puntuacio <= 5),
    comentari TEXT,
    descripcio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Taula d'Avisos
CREATE TABLE IF NOT EXISTS avisos (
    id VARCHAR(100) PRIMARY KEY,
    usuari_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    missatge TEXT NOT NULL,
    llegit BOOLEAN DEFAULT FALSE,
    data DATE DEFAULT CURRENT_DATE,
    enllac VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

