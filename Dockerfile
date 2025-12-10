# Build Stage
FROM maven:3.8.6 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Final Stage
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY --from=builder /app/target/tracking-0.0.2-SNAPSHOT.jar /app/tracking.jar

EXPOSE 8080
CMD ["java", "-jar", "tracking.jar"]

