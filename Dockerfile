FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY build/libs/*-SNAPSHOT.jar app.jar

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
