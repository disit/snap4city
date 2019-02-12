# Project Title

Snap4City MyPersonalData (aka DATAMANAGER)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

* Java JDK 1.8 or higher
* Maven 3.0 or higher
* Apache Tomcat 8
* MySQL 5.5 or higher

## Deployment

* Configure the properties file in src\main\resources. Three different profiles are available (local, deploy and test): Choose the one you prefere and note it down (1). The application properties file contains information about the used db (url, username and password) and endpoints for the identity providers (keycloak and ldap). The log4j properties file contains information about the logging of the application.
* From the home folder launch the command "mvn clean install -DskipTests". A file with "war" extension will be created in the target folder (i.e. target\datamanager-0.0.1.war)

### Installing

* Create the DB and its tables using the sql script available in src\main\resources\create_db_tables.sql (the name of the DB is by default "profiledb")
* Create a local folder whenever the logging files will be located (usually /var/log/tomcat8/mylogpath) and note it down the complete path (2)
* Configure the two enviroment variables for Apache Tomcat (edit setenv.sh in #apachetomcat_home#\bat or pass it via command line): -Dspring.profiles.active=local (as noted in (1)) -DlogFileFolder=/var/log/tomcat8/mylogpath (as noted in (2))
* Deploy the war file in Apache Tomcat 
* Test if the Snap4City MyPersonalData module is up and running tring to acceding via a web browser to the address http://<INSTALLED_URI>:<INSTALLED_PORT>/<TOMCAT_PATH>/api/test (i.e. http://localhost:8080/datamanager-0.0.1/api/test). It should return the label: "alive"

## Built With

* [Java](https://www.oracle.com) - Java Development Kit
* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Angelo Difino** - *Initial work* - [DISIT LAB](https://github.com/disit)
* **Claudio Badii** - *Contributor* - [DISIT LAB](https://github.com/disit)

## License

This project is licensed under the GNU Affero General Public License - see the [LICENSE.md](LICENSE) file for details