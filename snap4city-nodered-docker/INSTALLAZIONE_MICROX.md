# Installazione Snap4City Node-RED ADV 2.2.2 ARM64

## Requisiti

- MicroX con CPU ARM64 e Docker con Compose v2.
- Il nome `dashboard-test` deve essere raggiungibile dalla MicroX.
- Il client Keycloak `nodered` deve accettare il callback:
  `http://localhost:1880/nodered/nodered-arm64/auth/strategy/callback`.
- L'AppID `nodered-arm64` deve essere presente nell'Ownership della MicroX.

## Configurazione

1. Estrarre lo ZIP in una nuova directory.
2. Copiare `.env.microx.example` in `.env`.
3. Inserire in `.env` il `KEYCLOAK_CLIENT_SECRET`.
4. Generare e inserire una chiave stabile in `NODE_RED_CREDENTIAL_SECRET`. Non cambiarla dopo aver salvato credenziali nei flow.

## Build e avvio

```bash
docker compose --progress=plain build
docker compose up -d
```

La prima build scarica le dipendenze della palette ADV e può richiedere alcuni minuti.

## Verifica

```bash
docker compose ps
docker compose logs -f nodered
docker image inspect snap4city-nodered:2.2.2-arm --format '{{.Os}}/{{.Architecture}}'
```

Il risultato atteso è `linux/arm64` e lo stato del container deve diventare `healthy`.

Aprire:

```text
http://localhost:1880/nodered/nodered-arm64/
```

Accedere tramite Keycloak. Nei log, dopo un accesso autorizzato, devono comparire `SSO token exchange completed`, `He is a valid user` e, per il proprietario, `He is the owner`.

## Arresto e aggiornamento

```bash
docker compose down
docker compose build --pull
docker compose up -d
```

Il volume Docker `nodered-data` conserva flow e configurazioni. Non usare `docker compose down -v` se si vogliono mantenere i dati.

## Nota sull'immagine ADV

`disitlab/snap4city-nodered-v2.2.2-adv:v14` è pubblicata soltanto per AMD64. Questa build usa la base ufficiale Node-RED 2.2.2 per ARM64 e installa la palette ADV v14, incluso `node-red-contrib-snap4city-developer` 0.5.18.
