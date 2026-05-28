FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pagamento-service/pom.xml ./pom.xml

COPY pagamento-service/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
