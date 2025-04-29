#!/bin/bash

# chmod +x build-and-start.sh

set -e  # Exit immediately if any command fails

echo "🛠️  Building JAR..."
./mvnw clean package -DskipTests

echo "🐳 Starting services with Docker Compose..."
docker-compose up --build
