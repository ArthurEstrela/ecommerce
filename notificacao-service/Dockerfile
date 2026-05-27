FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY notificacao-service/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY notificacao-service/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
