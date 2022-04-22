FROM openjdk:11-jdk
#VOLUME /tmp
#WORKDIR /sampleProject
COPY service/target/sample-service.jar sample-service.jar
RUN wget -O dd-java-agent.jar 'https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.datadoghq&a=dd-java-agent&v=LATEST'
ENTRYPOINT ["java", "-javaagent:dd-java-agent.jar", "-Ddd.service.name=sample-service","-Ddd.logs.injection=true", "-jar", "sample-service.jar"]
EXPOSE 8080

