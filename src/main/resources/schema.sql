-- ==============================================================================
-- Esquema DDL per a la Base de Dades jjaux_db (PostgreSQL)
-- Plataforma AVISA / JJAUX
-- Totalment sincronitzat amb els models i serveis del Backend i Frontend
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
    verificat BOOLEAN DEFAULT FALSE,
    sollicitud_verificacio BOOLEAN DEFAULT FALSE,
    foto TEXT,
    foto_perfil TEXT,
    logo TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Taula d'Incidències (creades exclusivament per clients)
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

-- 3. Taula de Tasques (treballs en curs o finalitzats on convergeixen client i professional)
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Taula d'Ofertes (propostes i pressupostos presentats per professionals)
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

-- 5. Taula d'Avisos (notificacions del sistema als usuaris)
CREATE TABLE IF NOT EXISTS avisos (
    id VARCHAR(100) PRIMARY KEY,
    usuari_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    missatge TEXT NOT NULL,
    llegit BOOLEAN DEFAULT FALSE,
    data DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Taula de Ressenyes (valoracions de servei públiques de clients a professionals)
CREATE TABLE IF NOT EXISTS resenyes (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    client_nom VARCHAR(150),
    treballador_id VARCHAR(100) REFERENCES usuaris(id) ON DELETE CASCADE,
    treballador_nom VARCHAR(150),
    tasca_id VARCHAR(100) REFERENCES tasques(id) ON DELETE SET NULL,
    puntuacio INTEGER NOT NULL CHECK (puntuacio >= 1 AND puntuacio <= 5),
    descripcio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Taula de Blacklist de Telèfons (bloqueig de números per administració)
CREATE TABLE IF NOT EXISTS blacklist_telefons (
    id VARCHAR(100) PRIMARY KEY,
    telefon VARCHAR(50) UNIQUE NOT NULL,
    motiu TEXT,
    bloquejat_per VARCHAR(100) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Taula de Pressupostos (privats entre el client que obre la incidència i el professional que pressuposta)
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
