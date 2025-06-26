# Use official Eclipse Temurin JDK 24 base image 
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the compiled Spring Boot jar
COPY target/attendance.jar app.jar

# App listens on port 8443
EXPOSE 8443

# Set Timezone to Asia/Kolkata
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Run the Spring Boot jar
ENTRYPOINT ["java", "-jar", "app.jar"]
