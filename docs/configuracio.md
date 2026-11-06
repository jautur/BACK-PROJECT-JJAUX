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

- **Fora del perfil `dev`** (també amb `demo` tot sol i sense perfil) és obligatòria: si falta, el backend no arrenca. Mínim 10 caràcters.
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

Els usuaris de prova (`admin@admin.es` / `cli` / `pro`) es creen amb hash des dels seeders de `dev` i des de
`FRONT-PROJECT-JJAUX/scripts/buidar_db.sql` i `assegurar_admin.js`.

## Perfils i dades inicials

| Perfil | Què crea |
|---|---|
| Sense perfil, `demo` o qualsevol altre | Només l'administrador, amb `AVISA_ADMIN_PASSWORD` (obligatòria) |
| `dev` | L'administrador (amb la contrasenya de proves si falta la variable) i els usuaris de prova `cli` i `pro` |
| `dev,demo` | El mateix que `dev`, més les 10 empreses i els 10 clients de demostració (contrasenya `123456`) |

El perfil `demo` tot sol compta com a producció: no crea cap usuari amb contrasenya de proves (ho comprova `PerfilDemoIT`).
Per tindre les dades de demostració: `SPRING_PROFILES_ACTIVE=dev,demo ./desplegar.sh`.

## Límit d'intents de login

5 intents fallits en 15 minuts bloquegen aquell compte (l'identificador escrit, en minúscules) durant 15 minuts:
el login respon 429 amb `Retry-After`, encara que la contrasenya siga bona. Els comptes inexistents es bloquegen igual,
perquè la resposta no delate quins existeixen. No es limita per IP: darrere del proxy `serve.js` totes les peticions
arriben des de `127.0.0.1`, i es bloquejaria tothom alhora.

**Limitacions conegudes, per revisar quan despleguem:**

- **El comptador és en memòria.** Es perd en reiniciar el backend: un reinici desbloqueja tots els comptes.
- **Només val per a una instància.** Amb dues o més instàncies darrere d'un balancejador, cadascuna porta el seu
  comptador i els intents es multipliquen pel nombre d'instàncies. Caldria un magatzem compartit (la base o Redis).
- **Un tercer pot bloquejar un compte aliè.** Qui conega l'identificador d'algú pot fallar 5 vegades a propòsit i
  deixar-lo 15 minuts sense poder entrar, i repetir-ho. Per mitigar-ho caldria limitar també per IP real (cal que el
  proxy passe `X-Forwarded-For` i que el backend només se'n fie si ve del proxy), o demanar un CAPTCHA després de
  diverses fallades en lloc de bloquejar del tot.

## Imatges (fotos de perfil i logotips)

### `AVISA_IMATGES_DIRECTORI` (opcional)

Carpeta on es desen les imatges ja processades. Per defecte `~/.avisa/imatges`, fora dels repositoris. Si no existeix
es crea en arrencar; si no es pot crear, el backend no arrenca. Els noms dels fitxers els genera el backend
(UUID + `-petita` + `.jpg`/`.png`): cap nom ni camí que envie l'usuari arriba al disc.

### `AVISA_IMATGES_MAX_BYTES` (opcional)

Mida màxima del fitxer pujat, en bytes. Per defecte `5242880` (5 MB). Es comprova abans de mirar-ne el contingut;
si es passa, la resposta és 413. També fixa el límit del multipart del servidor (`ImatgesConfig`), que si no
seria l'1 MB per defecte de Spring Boot i tallaria fotos vàlides.

### `AVISA_IMATGES_MAX_PIXELS` (opcional)

Màxim d'amplària × altura de la imatge pujada. Per defecte `40000000` (40 megapíxels, prou per a qualsevol foto de mòbil).
Es llig només la capçalera i es rebutja (413) **abans de descodificar**: un PNG de pocs KB pot dir que fa 50000×50000 i
ocupar 10 GB en memòria (bomba de descompressió). Les imatges grans es lligen ja delmades, així que la memòria usada
depén poc d'aquest límit.

Els dos límits han de ser positius o el backend no arrenca.

### Què es fa amb cada imatge pujada (`ProcessadorImatges`)

1. Es reconeix el format pels primers bytes, no pel nom ni pel `Content-Type`. S'accepten JPEG, PNG i WebP;
   la resta (SVG, GIF, HEIC, PDF...) es rebutja amb 400, igual que un fitxer malmés o truncat.
2. S'aplica l'orientació EXIF, perquè les fotos fetes amb el mòbil girat es vegen dretes sense metadades.
3. Es generen dues mides, sense ampliar mai: **512 px** pel costat llarg (detall) i **128 px** (llistats, sufix `-petita`).
4. Es tornen a codificar des dels píxels, en JPEG (qualitat 0,85) o en PNG si la imatge té transparència.
   **No es copia cap metadada de l'original**: ni EXIF (ubicació GPS, model del mòbil, data), ni XMP, ni comentaris,
   ni el perfil de color, que es converteix a sRGB.

Lectors: TwelveMonkeys `imageio-jpeg` (JPEG CMYK i amb perfil ICC) i `imageio-webp` (només lectura: WebP s'accepta
d'entrada però mai se serveix); `metadata-extractor` només per llegir l'orientació.

### Endpoints

| Mètode i ruta | Qui | Resposta |
|---|---|---|
| `POST /api/users/{id}/foto` | Només el mateix usuari (qualsevol rol) | 200 `{"url", "urlPetita"}` |
| `POST /api/users/{id}/logo` | Només el mateix usuari, i ha de ser `PROFESSIONAL` | 200 `{"url", "urlPetita"}` |
| `DELETE /api/users/{id}/foto` i `/logo` | El mateix usuari o un administrador (moderació) | 204 |
| `GET /api/imatges/{nom}` | Tothom, sense sessió | La imatge |

- La pujada és `multipart/form-data` amb el fitxer al camp **`fitxer`**. El nom i el `Content-Type` que envia el
  navegador no es fan servir per a res.
- L'administrador pot esborrar la imatge de qualsevol usuari, però no en pot pujar cap en nom d'un altre (403).
- Errors: 400 si no és una imatge acceptada, 413 si és massa gran, 403 sense permís, 404 si l'usuari no existeix.
- **En pujar-ne una de nova o esborrar-la, els dos fitxers de l'anterior s'esborren del disc.** Quan l'administrador
  esborra un usuari, també s'esborren els de la seua foto i el seu logo.
- La base només guarda la URL gran (`foto_url`, `logo_url`). La petita és la mateixa amb `-petita` abans de
  l'extensió: `/api/imatges/<uuid>.jpg` → `/api/imatges/<uuid>-petita.jpg`.
- `GET /api/imatges/{nom}` és públic perquè un `<img src>` no envia el token. Els noms són UUID aleatoris: no es
  poden endevinar ni llistar. Com que un nom no canvia mai de contingut, es respon amb
  `Cache-Control: max-age=31536000, public, immutable`, i amb `X-Content-Type-Options: nosniff` i
  `Content-Security-Policy: default-src 'none'; sandbox` per si algun dia s'hi colara un contingut que no és imatge.

**Limitació coneguda:** si el procés s'atura just després de desar els fitxers i abans d'actualitzar la base, aquells
fitxers queden orfes al disc. No es veuen enlloc i no fan cap mal, però ocupen espai. Si mai cal, es poden netejar
comparant la carpeta amb `foto_url`/`logo_url`.
