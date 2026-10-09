# Build the React app
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Build the Spring Boot jar with the React build inside it
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/dist src/main/resources/static
RUN ./mvnw -B -q -DskipTests package

# Runtime: Debian 12 for Tesseract 5 + Leptonica 1.82, which Tess4J 5.7 expects
FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/opt/java
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk $JAVA_HOME
ENV PATH="$JAVA_HOME/bin:$PATH" \
    TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
RUN useradd --system --create-home app
USER app
WORKDIR /app
COPY --from=backend /app/target/hireshield-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-jar", "app.jar"]
