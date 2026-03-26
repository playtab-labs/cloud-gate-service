FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY build/libs/cloud-gate-service-*.jar app.jar

EXPOSE 8082 9094

ENTRYPOINT ["java", "-jar", "app.jar"]
