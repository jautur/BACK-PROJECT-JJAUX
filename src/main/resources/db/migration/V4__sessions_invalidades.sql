-- Un token JWT dura una setmana, però en canviar la contrasenya ha de deixar de valer.
-- Els tokens emesos abans d'aquest moment es rebutgen (vegeu ValidadorSessio). NULL = cap canvi.
ALTER TABLE usuaris
    ADD COLUMN sessions_valides_des_de TIMESTAMPTZ;
