# Verifiche eseguite

Data: 18 settembre 2026.

## Esito

Aggiornamento client pubblico: **7 test superati** con runner e runtime Node.js 22.23.2. Il test di avvio usa `KEYCLOAK_CLIENT_TYPE=public`, secret vuoto e client ID `nodered-edge`; controlla il redirect con challenge PKCE S256. Il nuovo test unitario controlla `token_endpoint_auth_method=none` e l'assenza di `client_secret` sia nel rinnovo durante il login sia nel job programmato. Login e scambio del codice contro il server reale restano da collaudare.

Prima dell'aggiornamento client pubblico, sei test superati su Windows x64. Il runner usa Node.js **22.23.2**; il processo Node-RED **2.2.2** e stato avviato anche con Node.js **16.13.1**, come nella base ufficiale. Lo script di applicazione overlay e stato eseguito con Node.js 16.13.1. Il test runtime verifica esplicitamente la versione 2.2.2 riportata all'avvio:

1. Ownership autorizza il proprietario e conserva il refresh token.
2. Ownership nega accesso per una app diversa.
3. Errori HTTP e risposte Ownership non valide non autorizzano l'utente.
4. Il login del delegato non sovrascrive il token del proprietario.
5. Username contenenti separatori di percorso sono rifiutati.
6. Avvio reale Node-RED, risposta 401 sulle API flows e refresh token senza login, endpoint di login SSO, redirect 302 verso Keycloak con parametro state e callback corretta, asset editor accessibile.

Le richieste Ownership sono simulate nei test unitari. Il test runtime usa host SSO fittizi e verifica il redirect senza contattare Keycloak. Non e un test completo del protocollo OIDC o della verifica delle firme dei token.

Il test runtime su Windows disabilita solo l'installazione dalla palette: Node-RED 2.2.2 usa una chiamata a npm.cmd incompatibile con Node.js moderno su Windows. Questa modifica e nel solo harness di test; nel Docker Linux la palette rimane abilitata. Import/export Resource Manager e funzionamento dei singoli nodi Snap4City non sono stati provati.

Il runner dei test richiede Node.js **22**. Per verificare il processo applicativo con un altro eseguibile impostare `RUNTIME_NODE` al suo percorso; il Docker usa Node.js 16.13.1. Node.js 24 non e stato collaudato con questo aggiornamento. I test locali usano le dipendenze del lockfile principale, non il filesystem completo dell'immagine ufficiale.

## Non ancora verificato

- Build ed esecuzione del container Linux ARM: motore Docker non disponibile nell'ambiente di preparazione.
- Login effettivo, refresh offline e autorizzazioni contro Keycloak/Ownership della destinazione: parametri non forniti.
- Import/export e installazione dei nodi dal Resource Manager reale.
- Nodi applicativi, connessioni a dashboard, database, broker e altri servizi esterni.

Verificati su Docker Hub i tag ufficiali `nodered/node-red:2.2.2` e `2.2.2-16`: includono ARMv7 e ARM64. La configurazione ARMv7 di `2.2.2-16` conferma Node.js 16.13.1, Alpine, utente `node-red` e installazione in `/usr/src/node-red`. Il Dockerfile usa questa base, installa solo le estensioni dal lockfile `docker/package-lock.json` e applica l'overlay ai moduli gia presenti. La disponibilita della base non costituisce una verifica della build applicativa.
