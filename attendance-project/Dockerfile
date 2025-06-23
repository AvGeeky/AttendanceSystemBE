# Use official Eclipse Temurin JDK 24 base image 
FROM eclipse-temurin:24-jdk

# Set working directory
WORKDIR /app

# Copy the compiled Spring Boot jar
COPY target/attendance.jar app.jar

# App listens on port 8443
EXPOSE 8443

# Run the Spring Boot jar
ENTRYPOINT ["java", "-jar", "app.jar"]
