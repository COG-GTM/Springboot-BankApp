#!/bin/bash
# Validate rollback capability
echo "Testing rollback to Java 17..."
./migration-scripts/rollback-java17.sh
mvn clean package
if [ $? -eq 0 ]; then
    echo "Rollback validation successful"
else
    echo "Rollback validation failed"
    exit 1
fi
