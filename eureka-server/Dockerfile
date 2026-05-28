FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY eureka-server/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY eureka-server/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
