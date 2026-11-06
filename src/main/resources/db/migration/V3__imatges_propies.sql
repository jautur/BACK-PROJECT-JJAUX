-- Imatges pròpies: foto de perfil i logotip, desades pel backend amb nom UUID.
-- Substitueixen les URLs externes (foto, foto_perfil, logo), que es descarten:
-- carregaven imatges de tercers i permetien injectar CSS des del perfil.
ALTER TABLE usuaris
    ADD COLUMN foto_url TEXT,
    ADD COLUMN logo_url TEXT;

ALTER TABLE usuaris
    ADD CONSTRAINT usuaris_foto_url_propia CHECK (foto_url IS NULL OR foto_url ~ '^/api/imatges/[0-9a-f-]{36}\.(jpg|png)$'),
    ADD CONSTRAINT usuaris_logo_url_propia CHECK (logo_url IS NULL OR logo_url ~ '^/api/imatges/[0-9a-f-]{36}\.(jpg|png)$');

ALTER TABLE usuaris
    DROP COLUMN foto,
    DROP COLUMN foto_perfil,
    DROP COLUMN logo;
