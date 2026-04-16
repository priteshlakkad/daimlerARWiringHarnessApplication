#!/bin/bash

# Ensure script stops on first error
set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "Building the Spring Boot project using Maven Wrapper with Java 17..."
./mvnw clean package -DskipTests

echo ""
echo "Build finished successfully!"
echo "You can find your JAR file inside the 'target' directory."
# Optionally provide a command to run the built jar
# echo "To run the application, use: java -jar target/<your-jar-file>.jar"
