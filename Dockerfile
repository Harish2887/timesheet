# Multi-stage build for the timesheet application
FROM node:20.10.0-alpine AS frontend-build

# Set working directory for frontend
WORKDIR /app/frontend

# Copy frontend package files
COPY frontend/package*.json ./

# Install frontend dependencies
RUN npm ci --only=production

# Copy frontend source code
COPY frontend/ ./

# Build frontend
RUN npm run build

# Backend build stage
FROM maven:3.9.5-openjdk-17-slim AS backend-build

# Set working directory
WORKDIR /app

# Copy backend pom.xml and maven settings
COPY backend/pom.xml ./backend/
COPY backend/maven-settings.xml ./backend/

# Copy frontend build from previous stage
COPY --from=frontend-build /app/frontend/build ./frontend/build

# Download dependencies (this layer will be cached if pom.xml doesn't change)
WORKDIR /app/backend
RUN mvn dependency:go-offline -B

# Copy backend source code
COPY backend/src ./src

# Build the application (skip frontend build since we already have it)
RUN mvn clean package -DskipTests -Dfrontend.skip=true

# Runtime stage
FROM openjdk:17-jre-slim

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Create directories for uploads and logs
RUN mkdir -p /app/uploads/invoices /app/logs && \
    chown -R appuser:appuser /app

# Copy the built jar from backend-build stage
COPY --from=backend-build /app/backend/target/timesheet-*.jar app.jar

# Copy static resources (frontend build) if not already included in jar
COPY --from=frontend-build /app/frontend/build ./static/

# Change ownership of application files
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
