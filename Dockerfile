FROM node:20-alpine AS web
WORKDIR /src
COPY web-admin/package*.json web-admin/
RUN cd web-admin && npm ci
COPY tools ./tools
COPY web-admin ./web-admin
RUN cd web-admin && npm run build && npm run build:portal

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /src
COPY backend/pom.xml backend/
RUN mvn -f backend/pom.xml -B dependency:go-offline
COPY backend ./backend
RUN mvn -f backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=backend /src/backend/target/laminar-ops-1.0.0.jar app.jar
COPY --from=web /src/web-admin/dist /app/web-admin/dist
COPY --from=web /src/web-admin/dist-portal /app/web-admin/dist-portal
ENV WEB_ADMIN_DIST=/app/web-admin/dist \
    WEB_PORTAL_DIST=/app/web-admin/dist-portal \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
