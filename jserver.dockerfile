FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /jserver-work
COPY jserver ./jserver
WORKDIR /app/jserver
RUN mvn clean package
FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y build-essential g++ gcc
WORKDIR /jserver-work
COPY --from=build /app/jserver/target/*-jar-with-dependencies.jar ./jserver.jar

EXPOSE 12345
CMD ["sh", "-c", "java -jar jserver.jar --host=0.0.0.0 --port=12345 --kwfile=keywords.txt --std=c++11"]
