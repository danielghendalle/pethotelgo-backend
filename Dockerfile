# Multi-stage Dockerfile for PetHotelGo with Firebase Auth
# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
# Copy sources
COPY src ./src
# Download dependencies and build the jar
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built JAR from build stage
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/target/*.jar /app/app.jar

# JVM options for production
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Firebase credentials directory (mount volume if using file-based auth)
ENV FIREBASE_CREDENTIALS_PATH=/app/firebase-credentials.json

# Application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher -c "echo" || exit 1

# Start application with Firebase and Spring profiles
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
  -Dfirebase.credentials.path=${FIREBASE_CREDENTIALS_PATH} \
  -jar /app/app.jar"]
