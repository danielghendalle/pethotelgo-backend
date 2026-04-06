# Multi-stage Dockerfile for PetHotelGo with Firebase Auth
# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Firebase credentials can be provided either:
#   - As base64 JSON string via FIREBASE_CREDENTIALS_BASE64 env var (recommended for cloud)
#   - As a mounted file via FIREBASE_CREDENTIALS_PATH env var (Docker volume / local)
ENV FIREBASE_CREDENTIALS_PATH=""

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher -c "echo" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
  -Dfirebase.credentials.path=${FIREBASE_CREDENTIALS_PATH} \
  -jar /app/app.jar"]
