# Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Final Runtime Stage (Lightweight)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=builder /app/target/tracking-0.0.2-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
