#!/bin/bash

# Run Spring Boot application with JVM arguments to fix Firebase module access issues
export MAVEN_OPTS="--add-opens java.base/java.time.chrono=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

./mvnw spring-boot:run