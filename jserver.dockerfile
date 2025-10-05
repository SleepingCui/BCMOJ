FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app/jserver
COPY jserver ./
RUN apt-get update && apt-get install -y --no-install-recommends build-essential g++ gcc \
    && mvn clean package -DskipTests \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:17-jdk
WORKDIR /jserver-work
COPY --from=build /app/jserver/target/*-jar-with-dependencies.jar ./jserver.jar

RUN useradd -m judge
USER judge

EXPOSE 12345
ENTRYPOINT ["java", "-jar", "jserver.jar"]
CMD ["--host=0.0.0.0", "--port=12345", "--kwfile=keywords.txt", "--std=c++11"]
