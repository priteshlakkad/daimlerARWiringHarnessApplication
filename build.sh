#!/bin/bash

# Ensure script stops on first error
set -e

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "Building the Spring Boot project using Maven Wrapper with Java 17..."
./mvnw clean package -DskipTests

JAR=$(ls target/*.jar | grep -v 'original' | head -1)

echo ""
echo "Build finished successfully!"
echo "JAR: $JAR"
echo ""
echo "To run the application:"
echo "  java -jar $JAR"
