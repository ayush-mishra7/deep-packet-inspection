# Stage 1: Build the application using Maven and OpenJDK 17
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the entire project
COPY . .

# Build the project, skipping tests for faster image creation
RUN mvn clean package -f dpi-engine/pom.xml -DskipTests

# Stage 2: Create a lightweight runtime image using OpenJDK 17
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Non-root user for security
RUN groupadd -r dpi && useradd -r -g dpi dpiuser

# Copy the built jar from the builder stage
# Assuming the main app is in dpi-engine/target/
COPY --from=builder /app/dpi-engine/target/*.jar app.jar

# Install libpcap for PCAP processing
RUN apt-get update && \
    apt-get install -y libpcap0.8 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Fix permissions
RUN chown dpiuser:dpi /app/app.jar

USER dpiuser

EXPOSE 8080

# Environment variables that can be overridden
ENV DPI_MODE=server
ENV DPI_WORKER_COUNT=4
ENV DPI_PCAP_FILE=""

ENTRYPOINT ["java", "-jar", "app.jar"]
