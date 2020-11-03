FROM tomcat:9.0.26-jdk8-openjdk-slim 

RUN sed -i "s|</Context>|<Valve className=\"org.apache.catalina.valves.rewrite.RewriteValve\"/></Context> |g" /usr/local/tomcat/conf/context.xml 
RUN echo "RewriteRule /v1/(.*) /orionbrokerfilter/v1/\$1" >> /usr/local/tomcat/webapps/ROOT/WEB-INF/rewrite.config 
RUN echo "RewriteRule /v2/(.*) /orionbrokerfilter/v2/\$1" >> /usr/local/tomcat/webapps/ROOT/WEB-INF/rewrite.config

RUN mkdir /usr/local/tomcat/credentials
RUN sed -i "s|<!-- Define an AJP 1.3 Connector on port 8009 -->|<Connector allowHostHeaderMismatch=\"true\" SSLEnabled=\"true\" clientAuth=\"want\" truststoreFile=\"credentials/caCerts.jks\" truststorePass=\"\${mytruststorepass}\" keystoreFile=\"credentials/tomcat.keystore\" keystorePass=\"\${mykeystorepass}\" maxThreads=\"200\" port=\"8443\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\" scheme=\"https\" secure=\"true\" sslProtocol=\"TLS\" sslEnabledProtocols=\"TLSv1.3,TLSv1.2,TLSv1.1\" ciphers=\"TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA\"/><!-- Define an AJP 1.3 Connector on port 8009 --> |g" /usr/local/tomcat/conf/server.xml

RUN mkdir /usr/local/tomcat/logs/orionbrokerfilter
COPY target/orionbrokerfilter-0.0.1.war /usr/local/tomcat/webapps/orionbrokerfilter.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
