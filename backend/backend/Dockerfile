# backend/Dockerfile
FROM eclipse-temurin:25-jre-alpine

# Copy the keystore into the image (you can also mount it as a volume)
COPY src/main/resources/localhost.p12 /app/localhost.p12

# Copy the built JAR
COPY target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app

# Expose the HTTPS port
EXPOSE 8443

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]