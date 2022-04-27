FROM openjdk:17
MAINTAINER Elizaveta Belokopytova
COPY build/libs/rate-limiter-0.0.1-SNAPSHOT.jar rate-limiter-0.0.1.jar
ENTRYPOINT ["java","-jar","/rate-limiter-0.0.1.jar"]