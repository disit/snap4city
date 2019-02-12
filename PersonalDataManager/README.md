# Project Title

Snap4City MyPersonalData (aka DATAMANAGER)

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

Java JDK 1.8 or higher
Maven 3.0 or higher
Apache Tomcat 8
MySQL 5.5 or higher

## Deployment

Configure the properties file in src\main\resources. There are three different profiles you can use (local, deploy and test). Configure the one you prefere and note down the choosen profile name. The application properties file contains information about the used db (url, username and password) and identity providers (keycloak and ldap). The log4j properties file contains information about the logging of the application.
From the home folder launch the command "mvn clean install -DskipTests". A file with extensione war will be created in the target folder (i.e. target\datamanager-0.0.1.war)

### Installing

To build 

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

## Built With

* [Java](https://www.oracle.com) - Java Development Kit
* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Angelo Difino** - *Initial work* - [DISIT LAB](https://github.com/disit)
* **Claudio Badii** - *Contributor* - [DISIT LAB](https://github.com/disit)

## License

This project is licensed under the GNU Affero General Public License - see the [LICENSE.md](LICENSE.md) file for details

