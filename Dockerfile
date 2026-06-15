# ================================================
# Revaro – Dockerfile
# Multi-stage build: build with Maven, run with JRE
# ================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user for security
RUN addgroup -S revaro && adduser -S revaro -G revaro

# Create uploads directory
RUN mkdir -p /app/uploads && chown revaro:revaro /app/uploads

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

USER revaro

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=docker", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
