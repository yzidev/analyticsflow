FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/analyticsflow-0.0.1-SNAPSHOT.jar /app/analyticsflow.jar

RUN mkdir -p /app/data/files /app/data/sample /app/data/reports

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/analyticsflow.jar"]
