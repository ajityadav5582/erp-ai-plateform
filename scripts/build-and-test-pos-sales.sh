#!/bin/bash
set -e

echo "Building POS and Sales modules..."
./gradlew :business:pos:application:compileJava :business:sales:application:compileJava --offline

echo "Running tests..."
./gradlew :business:pos:application:test :business:sales:application:test --offline

echo "Build and tests completed successfully."
