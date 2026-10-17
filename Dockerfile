# Builds the whole app (React + Spring Boot) into one image, e.g. for Render.
# The React build is served by Spring Boot, so the site and the API share one address.

# 1. Build the React app
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Build the Spring Boot jar with the React app inside it
FROM eclipse-temurin:25-jdk AS backend
WORKDIR /app/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/frontend/dist src/main/resources/static
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar /app/app.jar

# 3. Run it
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=backend /app/app.jar app.jar
# "Today" for budgets and recurring bills follows Indian time
ENV TZ=Asia/Kolkata
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
