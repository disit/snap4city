# Project Title

Snap4City Orion Broker Filter

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Java JDK 1.8 or higher
* Maven 3.0 or higher
* Apache Tomcat 8
* MySQL 5.5 or higher
* fiware/orion

## Configure
- Create keyclock client id for orionbrokerfilter
>client ID: orionbrokerfilter  
>TODO --> use also the secret: <orionbrokerfilter-secret>  
- Create in LDAP a user "rootfilter" with _RootAdmin_ rule
>-CN=rootfilter  
>-objectClass=inetOrgPerson, posixAccount, top  
-configure rewrite rule for tomcat
>-add in conf/context.xml  
>Valve className="org.apache.catalina.valves.rewrite.RewriteValve" />  
>-add in /usr/tomcat8/webapps/ROOT/WEB-INF/rewrite.config  
>RewriteRule /v1/(.*) /orionbrokerfilter/v1/$1  

## Deployment

* Configure the properties file in src\main\resources. Three different profiles are available (local, deploy and test): Choose the one you prefere and note it down (1). The application properties file contains information about the endpoints for the identity providers (keycloak and ldap) and spring.prefixelementID identiofy the organization server by this filter and the name of the contextbroker. The log4j properties file contains information about the logging of the application.
* From the home folder launch the command "mvn clean install -DskipTests". A file with "war" extension will be created in the target folder (i.e. target\orionbrokerfilter-0.0.1.war)

### Installing

* Create a local folder whenever the logging files will be located (usually /var/log/tomcat8/orionbrokerfilter) and note it down the complete path (2). To avoid more configure we suggest to use the inner final destination folder named "orionbrokerfilter"
* Configure the two enviroment variables for Apache Tomcat (edit setenv.sh usually in /usr/share/tomcat8 or pass it via command line): -Dspring.profiles.active=local (as noted in (1)) -DlogFileFolder=/var/log/tomcat8/ (as noted in (2) without the inner final "/orionbrokerfilter")
* Deploy the war file in Apache Tomcat 
* Test if the Snap4City Orion Broker Filter module is up and running tring to acceding via a web browser to the address http://<INSTALLED_URI>:<INSTALLED_PORT>/<TOMCAT_PATH>/api/test (i.e. http://localhost:8080/orionbrokerfilter-0.0.1/api/test). It should return the label: "alive"

## Configure for IoT Directory
- To configure the IoT Directory to use this module, please refer the section "(optional) Configure Orion Broker" in "https://github.com/disit/iot-directory/"

## Verify certificate exchange
- Create a new device that require cetificate exchange. Download the certificate and key and convert in P12
- In firefox, options, under privacy and security, show certificate, import the p12
- In firefox digit, digit https://iotobsf:8443/ and select the identity to check. 
- In firefox, to reset the selected identity, delete cert8.db

## Built With

* [Java](https://www.oracle.com) - Java Development Kit
* [Maven](https://maven.apache.org/) - Dependency Management

## How to install Fiware/Orion via docker-compose

- using root user, install docker compose like https://docs.docker.com/compose/install/ (prerequisite docker like https://docs.docker.com/install/linux/docker-ce/debian/)

- create docker-compose yaml file
>root@debian:/home/debian# mkdir fiware  
>root@debian:/home/debian# cd fiware  
>root@debian:/home/debian/fiware# mkdir db  
>(maybe not needed?)root@debian:/home/debian/fiware# docker volume create --name=mongodb_data_volume  
>root@debian:/home/debian/fiware# vi docker-compose  
>    version: "3.7"  
>    services:  
>       orion:  
>          image: fiware/orion  
>          depends_on:  
>            - mongo  
>          ports:  
>            - "1026:1026"  
>          command: -dbhost mongo  
>          restart: always  
>       mongo:  
>          image: mongo:3.6  
>          restart: always  
>          volumes:  
>            - /home/debian/fiware/db:/data/db  
- run docker-compose
>root@debian:/home/debian/fiware# docker-compose up
-Test if everything works well accessing http://iotobsf:1026/version

## Authors

* **Angelo Difino** - *Initial work* - [DISIT LAB](https://github.com/disit)

## License

This project is licensed under the GNU Affero General Public License - see the [LICENSE.md](LICENSE) file for details