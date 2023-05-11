FROM amazoncorretto:17-alpine-jdk
WORKDIR /tmp

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/tmp/app.jar"]