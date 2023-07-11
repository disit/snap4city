A differenza della versione 0.17.5 questa va sostituito alla cartella @Node-RED che si trova dentro node-modules.

1. Creare cartella con il nome che si vuole dare al container all'interno della cartella /root/nr-datadirs/ e dargli i permessi corretti

 mkdir /root/nr-datadirs/nodered-v2.2.2-adv
 chmod a+w /root/nr-datadirs/nodered-v2.2.2-adv
 chown -R 1001.1001 /root/nr-datadirs/nodered-v2.2.2-adv

2. Copiare nella cartella il file /mnt/data/nr-data/settings.js.tpl e modificarlo in modo tale che al posto di __NRID__ ci sia il nome del container

 cp /mnt/data/nr-data/settings.js.tpl /root/nr-datadirs/nodered-v2.2.2-adv/settings.js
 __NRID__ -> nodered-v2.2.2-adv

3. Creazione del container della nuova nr-adv a partire dall'immagine snap4city-nodered-v2.2.2-adv:v32

 docker run --publish 1911:1880 --detach --name nodered-v2.2.2-adv -v /root/nr-datadirs/nodered-v2.2.2-adv:/data snap4city-nodered-v1.1.3-adv:v32


3bis. Controllare con "docker ps" se la porta che si deve usare dopo --publish (1911) sia libera deve essere la stessa da inserire
	nel file indicato al putno 4.

4. Aggiungere nella cartella /root/nr-proxy il file nodered-v2.2.2-adv.conf copiandone un altro 
 
 cp nodered-v2.2.2-basic.conf nodered-v2.2.2-adv.conf

 e modificando i dati con il seguente
 
         location  /nodered/nodered-v2.2.2-adv {
                proxy_set_header Host $http_host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_pass "http://192.168.1.82:1911";
        }

5. Rilanciare nginx

  service nginx reload
  
6. Aggiungere la iotapp alla tabella ownership

'43443', 'badii', 'nodered-v2.2.2-adv', 'AppID', 'IOTApp adv based on Nodered v 2.2.2', 'https://iot-app.snap4city.org/nodered/nodered-v2.2.2-adv', '{\"type\":\"adv\",\"image\":\"snap4city-nodered-v1.1.3-adv:v32\",\"iotappids\":[]}', NULL, '2022-06-24 22:09:00', NULL, NULL

6b. Aggiornare NPM
    Aggiornare i Nodi
    Aggiornare se necessario debian
	- apt-get update
        - apt-get ugrade
        - apt-get dist-upgrade
        - cambiare /etc/apt/sources.list
	- eseguire nuovamente i comandi

7. Una volta aggiornato NPM

docker cp /home/debian/snap4city-user-authentication nodered-v2.2.2-adv:/usr/src/node-red/node_modules
docker cp /home/debian/importMicroServicesNodered/2.2.2/\@node-red nodered-v2.2.2-adv:/usr/src/node-red/node_modules

8.Per salvare e fare una nuova immagine
docker commit nodered-v2.2.2-adv snap4city-nodered-v2.2.2-adv:v1
docker save snap4city-nodered-v2.2.2-adv:v1 | gzip > /mnt/data/imgss/snap4city-nodered-v2.2.2-adv-v1.tgz

  
  
  ###COMANDI UTILI
   docker restart nodered-v2.2.2-adv
   docker logs --tail 100 nodered-v2.2.2-adv
   docker exec -it --user=root nodered-v2.2.2-adv /bin/bash

