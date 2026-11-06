-- Les contrasenyes passen a guardar-se amb BCrypt.
-- Les que encara siguen text pla es converteixen ací, i a partir d'ara la base no n'accepta cap sense hash.
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

UPDATE usuaris
SET password = public.crypt(password, public.gen_salt('bf', 10))
WHERE password !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$';

ALTER TABLE usuaris
    ADD CONSTRAINT usuaris_password_bcrypt CHECK (password ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$');
