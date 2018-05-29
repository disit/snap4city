call mvn clean install tomcat7:redeploy -Dtomcat-server=remote-deploy-orion-8 -DlogFileFolder=c:\logs -Dspring.profiles.active=local -DskipTests
call mvn tomcat7:redeploy -Dtomcat-server=remote-deploy-orion-9 -DlogFileFolder=c:\logs -Dspring.profiles.active=local -DskipTests
call mvn tomcat7:redeploy -Dtomcat-server=remote-deploy-orion-10 -DlogFileFolder=c:\logs -Dspring.profiles.active=local -DskipTests
call pscp.exe -pw ubuntu "target\classes\static\docs\index.html" ubuntu@192.168.0.17:/home/ubuntu/apis/orionbrokerfilter