ant do-clean do-dist -Dj2ee.server.home=/usr/share/tomcat8/ -Dplatforms.JDK_1.8.home=/usr/lib/jvm/java-8-openjdk-amd64 -Dlibs.CopyLibs.classpath=./extra/org-netbeans-modules-java-j2seproject-copylibstask.jar
cp dist/iotdeviceapi.war ../../servicemap/
