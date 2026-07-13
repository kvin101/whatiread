FROM eclipse-temurin:21.0.7_6-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21.0.7_6-jre-alpine
WORKDIR /app
RUN addgroup -g 10001 -S whatiread && adduser -u 10001 -S whatiread -G whatiread \
    && apk add --no-cache wget \
    && apk upgrade --no-cache \
    && chown whatiread:whatiread /app
USER 10001:10001
COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=8 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
