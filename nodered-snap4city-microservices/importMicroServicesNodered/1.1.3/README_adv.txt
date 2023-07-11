A differenza della versione 0.17.5 questa va sostituito alla cartella @Node-RED che si trova dentro node-modules.

1. Creare cartella con il nome che si vuole dare al container all'interno della cartella /root/nr-datadirs/ e dargli i permessi corretti

 mkdir /root/nr-datadirs/nodered-v1.1.3-adv
 chmod a+w /root/nr-datadirs/nodered-v1.1.3-adv
 chown -R 1001.1001 /root/nr-datadirs/nodered-v1.1.3-adv

2. Copiare nella cartella il file /mnt/data/nr-data/settings.js.tpl e modificarlo in modo tale che al posto di __NRID__ ci sia il nome del container

 cp /mnt/data/nr-data/settings.js.tpl /root/nr-datadirs/nodered-v1.1.3-adv/settings.js
 __NRID__ -> nodered-v1.1.3-adv

3. Creazione del container della nuova nr-adv a partire dall'immagine snap4city-nodered-adv:v90

 docker run --publish 1908:1880 --detach --name nodered-v1.1.3-adv -v /root/nr-datadirs/nodered-v1.1.3-adv:/data  snap4city-nodered-adv:v90

4. Aggiungere nella cartella /root/nr-proxy il file 

 nodered-v1.1.3-adv.conf
 
 contenente 
 
         location  /nodered/nodered-v1.1.3-adv {
                proxy_set_header Host $http_host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme;
                proxy_http_version 1.1;
                proxy_set_header Upgrade $http_upgrade;
                proxy_set_header Connection "upgrade";
                proxy_pass "http://192.168.1.82:1908";
        }

5. Rilanciare nginx

  service nginx reload
  
6. Aggiungere la iotapp alla tabella ownership

'20906', 'roottooladmin1', 'nodered-v1.1.3-adv', 'AppID', 'IOTApp adv based on Nodered v 1.1.3', 'https://iot-app.snap4city.org/nodered/nodered-v1.1.3-adv', '{\"type\":\"adv\",\"image\":\"snap4city-nodered-adv:v90\",\"iotappids\":[]}', NULL, '2020-09-17 12:17:19', NULL, NULL

7. Una volta aggiornato NPM

docker cp /home/debian/snap4city-user-authentication nodered-v1.1.3-adv:/usr/src/node-red/node_modules
docker cp /home/debian/importMicroServicesNodered/1.1.3/\@node-red nodered-v1.1.3-adv:/usr/src/node-red/node_modules

8.Per salvare e fare una nuova immagine
docker commit nodered-v1.1.3-adv snap4city-nodered-v1.1.3-adv:v1
docker save snap4city-nodered-v1.1.3-adv:v1 | gzip > /mnt/data/imgs/snap4city-nodered-v1.1.3-adv-v1.tgz

  
  
  ###COMANDI UTILI
   docker restart nodered-v1.1.3-adv
   docker logs --tail 100 nodered-v1.1.3-adv
   docker exec -it --user=root nodered-v1.1.3-adv /bin/bash

