# Snap4City Node-RED per Docker ARM

Progetto Docker per riprodurre Node-RED **2.2.2** con l'editor modificato Snap4City descritto nel documento `Modifiche Interfaccia Node.docx`. Include import/export del Resource Manager, i nodi `node-red-contrib-snap4city-user` 0.9.64 e il modulo originale `snap4city-user-authentication`, con alcune correzioni elencate sotto. Il target predefinito e **linux/arm/v7**; e selezionabile anche **linux/arm64**.

## Stato della consegna

Il pacchetto contiene il progetto per costruire l'immagine. **Non contiene ancora un'immagine Docker compilata**: nell'ambiente di preparazione non e disponibile Docker. I test locali sono descritti in `VALIDATION.md`. Il login reale, i nodi applicativi e lo scambio con il Resource Manager richiedono i servizi della propria installazione Snap4City e non sono stati collaudati end-to-end.

Node-RED 2.2.2 usa l'overlay ufficiale Snap4City per la versione richiesta, con le funzioni descritte nel Word. Node-RED e varie dipendenze di questa versione sono obsolete: non considerare il solo superamento dei test una certificazione per l'esposizione pubblica.

## Creare il Docker

Sul dispositivo ARM con Docker e Compose:

```sh
cp .env.example .env
# Compilare .env prima dell'avvio.
docker compose build
docker compose up -d
docker compose logs -f
```

Per ARM64 impostare `DOCKER_PLATFORM=linux/arm64` nel file `.env`. ARMv7 richiede un sistema Linux a 32 bit compatibile; ARM64 richiede Linux a 64 bit. Il target segue l'architettura del sistema operativo.

Per ottenere un file Docker trasportabile, da una macchina con Buildx e supporto per il target:

```sh
sh scripts/build-arm.sh
# ARM64:
DOCKER_PLATFORM=linux/arm64 sh scripts/build-arm.sh
```

Su un host x86 la build ARM richiede l'emulazione configurata nel builder. Lo script genera `dist/snap4city-nodered-2.2.2-armv7.tar`, oppure il corrispondente file `arm64.tar`. Non pubblica nulla su un registry. Sul dispositivo di destinazione:

```sh
docker load -i dist/snap4city-nodered-2.2.2-armv7.tar
docker compose up -d --no-build
```

## Configurazione SSO

Compilare in `.env`:

- `APP_ID`: identificatore dell'IoT App registrato nell'Ownership, per esempio `nodered-arm7`.
- `PUBLIC_URL`: URL pubblico completo, per esempio `https://iot.example.org/nodered/nodered-arm7`. Deve terminare con `/APP_ID`.
- `KEYCLOAK_REALM_URL`: URL del realm, inclusa l'eventuale parte `/auth`, per esempio `https://sso.example.org/auth/realms/master`.
- `KEYCLOAK_CLIENT_ID`: client Keycloak, con Standard Flow abilitato.
- `KEYCLOAK_CLIENT_TYPE`: `public` per un client pubblico (ad esempio `nodered-edge`), oppure `confidential`.
- `KEYCLOAK_CLIENT_SECRET`: lasciare vuoto per un client pubblico; obbligatorio per un client confidential. Il login usa PKCE S256; per i client pubblici lo scambio e il rinnovo dei token non richiedono client secret.
- `OWNERSHIP_ENDPOINT`: endpoint di elenco Ownership, tipicamente `https://host/ownership-api/v1/list/`.
- `PROCESS_LOADER_URL`: endpoint API del Resource Manager/Process Loader della propria installazione.
- `NODE_RED_CREDENTIAL_SECRET`: segreto persistente per cifrare le credenziali dei flow. Conservarlo e non cambiarlo dopo aver salvato credenziali.

Registrare nel client Keycloak questo **Valid Redirect URI**, sostituendo l'host:

```text
https://iot.example.org/nodered/nodered-arm7/auth/strategy/callback
```

Consentire gli scope `openid profile offline_access` e il relativo accesso offline. Il modulo accetta `username` oppure `preferred_username` dal profilo. Se il realm storico richiede lo scope personalizzato `username`, usare `SSO_SCOPE=openid username profile offline_access`. I nomi utente supportati contengono lettere ASCII, cifre, `@`, `.`, `_` e `-`.

Registrare la IoT App nel servizio Ownership con `elementId` uguale a `APP_ID`, tipo `AppID`, proprietario e URL pubblico corretti, come indicato nel Word. Usare gli strumenti amministrativi della propria installazione: non copiare gli ID o i nomi utente di esempio del documento. Un login Keycloak valido senza autorizzazione Ownership viene rifiutato. Proprietario e delegati autorizzati ricevono permesso editor `*`, secondo il comportamento originale Snap4City.

Il refresh token del proprietario viene conservato in `/data/refresh_token` per i nodi Snap4City e rinnovato dal job originale giornaliero. I token temporanei per utente seguono il comportamento del modulo upstream e rimangono nel volume dati. L'SSO protegge editor e API amministrative: gli endpoint creati dai flow HTTP In richiedono la propria autenticazione applicativa.

## Proxy, dati e secret

Il container usa l'utente non-root `node-red` (UID 1000) e un volume persistente `/data`. La porta pubblicata e limitata a `127.0.0.1` per un reverse proxy sullo stesso host; l'esempio Nginx si trova in `config/nginx.conf.example`. Il proxy deve preservare il percorso pubblico e supportare WebSocket. Se il proxy e remoto, adeguare `BIND_ADDRESS` alla rete prevista.

`.env` e escluso da Git, dal contesto Docker e dall'archivio di consegna. I secret non entrano nell'immagine. E possibile usare `KEYCLOAK_CLIENT_SECRET_FILE` e `NODE_RED_CREDENTIAL_SECRET_FILE` al posto dei valori diretti, montando esplicitamente i file nel container. Non caricare il volume `/data` in Git o nell'immagine.

## Sorgenti e modifiche

Sorgenti inclusi da [disit/snap4city](https://github.com/disit/snap4city), commit `3d92fee1eb58cf3a979a3a5facbb44baf8da797a`:

- `nodered-snap4city-microservices/importMicroServicesNodered/2.2.2/@node-red`;
- `nodered-snap4city-microservices/snap4city-user-authentication`.

I file upstream sono conservati in `upstream/`, con licenze originali. `scripts/prepare-runtime.js` applica in modo ripetibile l'overlay alle dipendenze npm e modifica la copia installata:

- rimuove la stampa delle risposte contenenti token;
- accetta `preferred_username`, rifiuta nomi non sicuri nei percorsi e richiede un refresh token;
- codifica client ID, client secret e refresh token nei body delle richieste;
- rifiuta risposte Ownership con HTTP diverso da 200 o formato non array;
- espone all'editor solo `processLoaderUrl`, senza esportare i secret.

La configurazione segue anche il [template ufficiale Snap4City](https://github.com/disit/snap4city-docker/blob/master/DataCity-Small/iotapp-nr1/settings.js). La base e l'immagine ufficiale `nodered/node-red:2.2.2-16`, con Node.js 16.13.1 e Alpine Linux, disponibile per ARMv7 e ARM64. Node-RED gia presente nella base viene mantenuto e modificato con l'overlay Snap4City. Solo le estensioni sono installate separatamente in `/opt/snap4city`, con dipendenze fissate in `docker/package-lock.json`; `package-lock.json` serve al collaudo locale. Anche Node.js 16 della base e fuori supporto.

## Test locali

Con Node.js **22** e npm, la versione usata per il collaudo:

```sh
npm ci --omit=optional --ignore-scripts --no-audit --no-fund
npm run prepare:runtime
npm test
```

Il test runtime usa la porta locale 1880 e servizi SSO fittizi; non richiede credenziali reali. Dopo il deploy verificare login del proprietario, rifiuto di un utente non autorizzato, import/export dal Resource Manager e persistenza dei flow dopo un riavvio.
