#!/bin/bash
# Rollback script to revert to Java 17
# Uses backup files instead of git checkout (works after PR is merged)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cp "$SCRIPT_DIR/backup-pom.xml" "$(dirname "$SCRIPT_DIR")/pom.xml"
cp "$SCRIPT_DIR/backup-Dockerfile" "$(dirname "$SCRIPT_DIR")/Dockerfile"

echo "Rolled back to Java 17 configuration"
