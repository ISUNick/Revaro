FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy the pom first so dependencies stay cached until it changes
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S revaro && adduser -S revaro -G revaro
COPY --from=builder /app/target/*.jar app.jar
USER revaro

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
