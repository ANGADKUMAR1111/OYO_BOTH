# =============================================
# Stage 1 — Build the JAR with Maven
# =============================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first to cache dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# =============================================
# Stage 2 — Minimal runtime image
# =============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S oyo && adduser -S oyo -G oyo

# Copy the fat JAR from builder stage
COPY --from=builder /app/target/oyo-backend-1.0.0.jar app.jar

# Give ownership to non-root user
RUN chown oyo:oyo app.jar
USER oyo

# Render injects PORT env var; default to 8080 locally
EXPOSE 8080

# JVM tuning for low-memory containers (Render free plan = 512MB RAM)
ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx400m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
