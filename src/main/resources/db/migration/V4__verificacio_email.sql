-- Afegir estat de verificació d'email i taula per als tokens de verificació d'email

ALTER TABLE usuaris
    ADD COLUMN email_verificat BOOLEAN NOT NULL DEFAULT FALSE;

-- Marcar els usuaris existents com a verificats perquè no perdin accés
UPDATE usuaris SET email_verificat = TRUE;

CREATE TABLE tokens_verificacio_email (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuari_id UUID NOT NULL REFERENCES usuaris(id) ON DELETE CASCADE,
    token VARCHAR(128) NOT NULL,
    caducitat TIMESTAMP NOT NULL,
    creat_a TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX tokens_verificacio_token_unic ON tokens_verificacio_email (token);
CREATE INDEX tokens_verificacio_usuari ON tokens_verificacio_email (usuari_id);
