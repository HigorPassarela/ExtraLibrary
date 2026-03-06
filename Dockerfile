# ===================================
# MULTI-STAGE BUILD FOR RAILWAY
# ===================================

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

LABEL maintainer="ExtraLibrary Team"
LABEL description="ExtraLibrary API - Build Stage"

WORKDIR /app

# Copy Maven files first (better caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# ===================================
# PRODUCTION RUNTIME STAGE
# ===================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ExtraLibrary Team"
LABEL description="ExtraLibrary API - Production Runtime"
LABEL version="1.0"

WORKDIR /app

# Install required packages
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init && \
    cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    echo "America/Sao_Paulo" > /etc/timezone && \
    rm -rf /var/cache/apk/*

# Create non-root user for security
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown spring:spring /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true"

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]