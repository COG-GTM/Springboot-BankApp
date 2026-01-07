#!/bin/bash
# Automatic rollback if build fails
set -e

echo "Attempting Java 22 build..."
mvn clean package || {
    echo "Java 22 build failed, initiating rollback..."
    ./migration-scripts/rollback-java17.sh
    mvn clean package
    echo "Rolled back to Java 17 successfully"
    exit 1
}
echo "Java 22 build successful"
