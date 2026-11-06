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

## `AVISA_ADMIN_PASSWORD`

Contrasenya del compte d'administrador `admin@admin.es`, que crea `AdminSeeder` en arrencar (en tots els perfils).

- **Fora del perfil `dev`** (també `demo` i sense perfil) és obligatòria: si falta, el backend no arrenca. Mínim 10 caràcters.
- **En `dev`**, si no està definida, l'admin es crea amb la contrasenya de proves de sempre i, si ja existeix, no es toca.
- **Si està definida, mana ella:** si l'admin ja existeix amb una altra contrasenya, se li canvia per aquesta en arrencar.
  Canviar-la des de la pantalla no dura: al següent arrencada torna a la de la variable.

Els scripts de desplegament la prenen de l'entorn o del fitxer local `~/.avisa/admin_password`, igual que el secret JWT,
i s'aturen si falta fora de `dev`:

```bash
mkdir -p ~/.avisa && openssl rand -base64 24 > ~/.avisa/admin_password && chmod 600 ~/.avisa/admin_password
```

## `SPRING_DATASOURCE_PASSWORD`

Contrasenya de PostgreSQL. Només el perfil `dev` en té una per defecte (la de la base local); fora de `dev` cal definir-la
(els scripts `casa_*` ja ho fan). Cap credencial té valor per defecte a `application.properties`: ho comprova `ConfiguracioTest`.

## `AVISA_CORS_ORIGENS` (opcional)

Orígens que poden cridar l'API directament des d'un navegador, separats per comes (es permeten patrons com
`http://192.168.*.*:*`). Mai `*`: el backend no arrenca si hi és. Per defecte: localhost, 127.0.0.1 i la xarxa local,
que és com el front crida el port 8080 des d'un mòbil. El front servit per `serve.js` va pel mateix origen i no en depén.
No s'envien credencials CORS: la sessió viatja a la capçalera `Authorization`.

## Contrasenyes

Es guarden amb BCrypt. La migració `V2__contrasenyes_bcrypt.sql` converteix les que encara eren text pla
(fa servir l'extensió `pgcrypto` de PostgreSQL) i afegeix la restricció `usuaris_password_bcrypt`: a partir
d'aquí la base rebutja qualsevol contrasenya sense hash, també si la insereix un script.

Els usuaris de prova (`admin@admin.es` / `cli` / `pro`) es creen amb hash des dels seeders `dev`/`demo` i des de
`FRONT-PROJECT-JJAUX/scripts/buidar_db.sql` i `assegurar_admin.js`.
