FROM openjdk:17
VOLUME /tmp
EXPOSE 8080
ARG JAR_FILE=build/libs/naggr-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} app.jar
COPY . docker-compose.yml
CMD ["docker-compose","up"]
ENTRYPOINT ["java","-jar","/app.jar"]