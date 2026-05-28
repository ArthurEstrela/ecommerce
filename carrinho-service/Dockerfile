FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY grpc-contracts/pom.xml ./grpc-contracts/pom.xml
COPY grpc-contracts/src ./grpc-contracts/src
RUN mvn -q -f grpc-contracts/pom.xml -DskipTests install

COPY carrinho-service/pom.xml ./carrinho-service/pom.xml

COPY carrinho-service/src ./carrinho-service/src
RUN mvn -q -f carrinho-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/carrinho-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
