# Build stage
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Create volume mount point
RUN mkdir /app/uploads && \
    chown -R nobody:nogroup /app

# Switch to non-root user
USER nobody

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 
