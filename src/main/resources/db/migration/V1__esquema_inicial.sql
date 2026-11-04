-- Esquema inicial de AVISA. El disseny i les regles es descriuen a docs/disseny-esquema.md.

CREATE TABLE usuaris (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('CLIENT', 'PROFESSIONAL', 'ADMIN')),
    telefon VARCHAR(50),
    bio TEXT,
    percentil INT NOT NULL DEFAULT 3 CHECK (percentil BETWEEN 0 AND 100),
    verificat BOOLEAN NOT NULL DEFAULT FALSE,
    sollicitud_verificacio BOOLEAN NOT NULL DEFAULT FALSE,
    foto TEXT,
    foto_perfil TEXT,
    logo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX usuaris_email_unic ON usuaris (LOWER(email));
CREATE INDEX usuaris_rol ON usuaris (rol);

CREATE TABLE publicacions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipus VARCHAR(20) NOT NULL CHECK (tipus IN ('OFERTA', 'INCIDENCIA')),
    autor_id UUID NOT NULL REFERENCES usuaris(id),
    titol VARCHAR(255) NOT NULL,
    descripcio TEXT,
    categoria VARCHAR(100) NOT NULL,
    preu VARCHAR(100),
    estat VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT publicacions_estat_segons_tipus CHECK (
        (tipus = 'OFERTA' AND estat IN ('ACTIVA', 'RETIRADA'))
        OR (tipus = 'INCIDENCIA' AND estat IN ('OBERTA', 'ASSIGNADA', 'TANCADA'))
    )
);
CREATE INDEX publicacions_tipus_estat ON publicacions (tipus, estat);
CREATE INDEX publicacions_autor ON publicacions (autor_id);

CREATE TABLE pressupostos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacio_id UUID NOT NULL REFERENCES publicacions(id),
    professional_id UUID NOT NULL REFERENCES usuaris(id),
    preu VARCHAR(100) NOT NULL,
    descripcio TEXT,
    duracio_estimada VARCHAR(100),
    estat VARCHAR(20) NOT NULL DEFAULT 'PENDENT' CHECK (estat IN ('PENDENT', 'ACCEPTAT', 'REBUTJAT')),
    rebutjat_automaticament BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pressupostos_automatic_si_rebutjat CHECK (NOT rebutjat_automaticament OR estat = 'REBUTJAT'),
    CONSTRAINT pressupostos_un_per_professional UNIQUE (publicacio_id, professional_id)
);
CREATE UNIQUE INDEX pressupostos_un_acceptat ON pressupostos (publicacio_id) WHERE estat = 'ACCEPTAT';
CREATE INDEX pressupostos_professional ON pressupostos (professional_id);

CREATE TABLE tasques (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publicacio_id UUID NOT NULL REFERENCES publicacions(id),
    client_id UUID NOT NULL REFERENCES usuaris(id),
    professional_id UUID NOT NULL REFERENCES usuaris(id),
    pressupost_id UUID UNIQUE REFERENCES pressupostos(id),
    preu_acordat VARCHAR(100) NOT NULL,
    estat VARCHAR(20) NOT NULL CHECK (estat IN (
        'SOLLICITADA', 'REBUTJADA', 'RETIRADA', 'EN_CURS', 'EN_REVISIO', 'FINALITZADA', 'CANCELLADA'
    )),
    data_inici TIMESTAMP,
    data_finalitzacio TIMESTAMP,
    valoracio INT CHECK (valoracio BETWEEN 1 AND 5),
    comentari TEXT,
    data_valoracio TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tasques_parts_diferents CHECK (client_id <> professional_id),
    CONSTRAINT tasques_inici_si_acceptada CHECK (
        data_inici IS NOT NULL OR estat IN ('SOLLICITADA', 'REBUTJADA', 'RETIRADA')
    ),
    CONSTRAINT tasques_finalitzacio_coherent CHECK ((estat = 'FINALITZADA') = (data_finalitzacio IS NOT NULL)),
    CONSTRAINT tasques_valoracio_si_finalitzada CHECK (valoracio IS NULL OR estat = 'FINALITZADA'),
    CONSTRAINT tasques_data_valoracio_coherent CHECK ((valoracio IS NULL) = (data_valoracio IS NULL))
);
CREATE UNIQUE INDEX tasques_una_sollicitud_activa ON tasques (publicacio_id, client_id) WHERE estat = 'SOLLICITADA';
CREATE INDEX tasques_client ON tasques (client_id);
CREATE INDEX tasques_professional ON tasques (professional_id);
CREATE INDEX tasques_publicacio ON tasques (publicacio_id);

CREATE TABLE sollicituds_cancellacio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tasca_id UUID NOT NULL REFERENCES tasques(id),
    sollicitant_id UUID NOT NULL REFERENCES usuaris(id),
    motiu TEXT NOT NULL CHECK (motiu <> ''),
    resolucio VARCHAR(30) CHECK (resolucio IN ('SENSE_CONSEQUENCIES', 'NEGLIGENCIA', 'REBUTJADA')),
    responsable_id UUID REFERENCES usuaris(id),
    admin_id UUID REFERENCES usuaris(id),
    data_resolucio TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT cancellacio_responsable_si_negligencia CHECK ((resolucio = 'NEGLIGENCIA') = (responsable_id IS NOT NULL)),
    CONSTRAINT cancellacio_admin_si_resolta CHECK ((resolucio IS NULL) = (admin_id IS NULL)),
    CONSTRAINT cancellacio_data_si_resolta CHECK ((resolucio IS NULL) = (data_resolucio IS NULL))
);
CREATE UNIQUE INDEX cancellacio_una_pendent ON sollicituds_cancellacio (tasca_id) WHERE resolucio IS NULL;
CREATE INDEX cancellacio_responsable ON sollicituds_cancellacio (responsable_id);

CREATE TABLE avisos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destinatari_id UUID NOT NULL REFERENCES usuaris(id) ON DELETE CASCADE,
    emissor_id UUID REFERENCES usuaris(id) ON DELETE SET NULL,
    missatge TEXT NOT NULL,
    llegit BOOLEAN NOT NULL DEFAULT FALSE,
    enllac VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX avisos_destinatari ON avisos (destinatari_id);

CREATE TABLE blacklist_telefons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telefon VARCHAR(50) NOT NULL UNIQUE,
    motiu TEXT,
    bloquejat_per VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
