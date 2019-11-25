export IOTDIR_SERVER_COMMON_NAME=<orion_broker_common_name>
export IOTDIR_SERVER_PK12_PASSWORD=<pk12_password>
export IOTDIR_SERVER_TRUSTSTORE_PASSWORD=<server_truststore_password>

rm -r caCerts.jks 
rm -r tomcat.keystore 

keytool -import -alias CertAuth -keystore caCerts.jks -file ca-crt.pem -storepass $IOTDIR_SERVER_TRUSTSTORE_PASSWORD -noprompt

keytool -importkeystore -destkeystore tomcat.keystore -srckeystore server-$IOTDIR_SERVER_COMMON_NAME-crt.p12 -srcstoretype PKCS12 -alias 1 -srcstorepass $IOTDIR_SERVER_PK12_PASSWORD -deststorepass $IOTDIR_SERVER_PK12_PASSWORD 
