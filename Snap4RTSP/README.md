# Snap4RTSP

to build the image used in the docker compose use: "docker-compose build -t disitlab/snap4rtsp-server:v0 ."

in snap4cam folder there is a docker-compose.yml file to bring up kurento, coturn and snap4rtsp webapp, these containers are on the host network, 
some configurations are on the compose file, other configurations are in the coturn/turnserver.conf and snap4rtsp-conf folder

the snap4rtsp webapp need access to the Dashboard.camdata table of the dashboard builder and uses servicemap to check if the user can access a specific rtsp source. 
The rtsp url is retrieved from the camdata table. 

the webapp is available at https://<host>:8443/ and the websocket server is used to interact with kurento. The indication of the rtsp source to use is 
  provided on the page using the src query parameter as http://...:8443/?src=...serviceuri...
  
  where the serviceuri is used to retrive the rtsp data without allowing direct access to the rtsp url.
  
