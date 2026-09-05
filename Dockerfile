FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/ ./
COPY --from=frontend-build /workspace/backend/src/main/resources/static/ ./src/main/resources/static/
RUN mvn -q clean package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
COPY --from=backend-build --chown=app:app /workspace/backend/target/crypto-lab-*.jar app.jar
USER 10001
EXPOSE 8080 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

