FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY grpc-contracts/pom.xml ./grpc-contracts/pom.xml
COPY grpc-contracts/src ./grpc-contracts/src
RUN mvn -q -f grpc-contracts/pom.xml -DskipTests install

COPY pedido-service/pom.xml ./pedido-service/pom.xml
RUN mvn -q -f pedido-service/pom.xml -DskipTests dependency:go-offline

COPY pedido-service/src ./pedido-service/src
RUN mvn -q -f pedido-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/pedido-service/target/*.jar app.jar

EXPOSE 8082 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
