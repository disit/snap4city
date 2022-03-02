FROM tomcat:9.0.45-jdk11-openjdk-buster

WORKDIR $CATALINA_HOME 

EXPOSE 8080

RUN rm -rf $CATALINA_HOME/webapps/examples

ENV BIMSERVER_APP $CATALINA_HOME/webapps/
ENV BIMSERVER_WAR_URL https://github.com/opensourceBIM/BIMserver/releases/download/v1.5.182/bimserverwar-1.5.182.war


RUN apt-get update -y && \
    apt-get upgrade -y && \
    apt-get clean

RUN apt-get install build-essential -y

RUN set -x \
	&& wget "$BIMSERVER_WAR_URL" -O ROOT.war \	
	&& cp ROOT.war $BIMSERVER_APP \
	&& rm ROOT.war	\
	&& rm $CATALINA_HOME/conf/server.xml


COPY server.xml $CATALINA_HOME/conf/
RUN chown -R root $CATALINA_HOME/conf/server.xml

RUN mkdir /usr/local/bimserver \
    && mkdir /usr/local/bimserver/home \
    && chown -R root  /usr/local/bimserver/home

VOLUME /usr/local/bimserver/home

CMD catalina.sh run
