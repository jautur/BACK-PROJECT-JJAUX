# Configuració del backend

## `AVISA_JWT_SECRET` (obligatòria)

Secret amb què se signen i es comproven els tokens de sessió (JWT, HS256). Qui el coneix pot fabricar-se
un token de qualsevol usuari, també d'administrador, així que **mai no ha d'estar al repositori**.

- Si no està definida, o té menys de 32 caràcters, **el backend no arrenca**.
- Canviar-la invalida tots els tokens emesos: tothom ha de tornar a iniciar sessió.
- El secret que hi havia escrit al codi fins al 2026-11-06 és públic a l'historial de git i ja no val.

### Crear-la en una màquina nova

```bash
mkdir -p ~/.avisa
openssl rand -base64 48 > ~/.avisa/jwt_secret
chmod 600 ~/.avisa/jwt_secret
```

Els scripts de desplegament (`FRONT-PROJECT-JJAUX/scripts/*desplegar*.sh`) la prenen, per aquest ordre:

1. de la variable d'entorn `AVISA_JWT_SECRET`, si ja està definida;
2. del fitxer `~/.avisa/jwt_secret`, que viu fora dels repositoris.

Si no la troben en cap dels dos llocs, s'aturen amb un missatge.

Per arrencar el jar a mà: `AVISA_JWT_SECRET="$(cat ~/.avisa/jwt_secret)" java -jar target/jjaux-back-0.0.1-SNAPSHOT.jar`.

### Tests

Els tests en fan servir un de propi, definit a `src/test/resources/application.properties`, que no val per a res
més. La integració contínua no necessita cap secret.

## Contrasenyes

Es guarden amb BCrypt. La migració `V2__contrasenyes_bcrypt.sql` converteix les que encara eren text pla
(fa servir l'extensió `pgcrypto` de PostgreSQL) i afegeix la restricció `usuaris_password_bcrypt`: a partir
d'aquí la base rebutja qualsevol contrasenya sense hash, també si la insereix un script.

Els usuaris de prova (`admin@admin.es` / `cli` / `pro`) es creen amb hash des dels seeders `dev`/`demo` i des de
`FRONT-PROJECT-JJAUX/scripts/buidar_db.sql` i `assegurar_admin.js`.
