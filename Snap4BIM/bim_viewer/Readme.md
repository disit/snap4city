## Mysql configuration 
- server/db/mysql.js

## Mysql create pins table
- server/db/create_pins_schema.sql

## BIMserver address configuration 
Url location where to find bimserver (default http://localhost:8080)
- bimsurfer/public/address.js

## BIMserver docker image,  latest stable version 1.5.162
- docker pull disitlab/bimserver:1.5.162

## Application entry points
This is the page used to add/remove pins
- http://localhost:8081/public/add.html

This is the page used only as viewer
- http://localhost:8081/public/view.html?poid=(the project poid)

NB: use username/password of bimserver on the login page





