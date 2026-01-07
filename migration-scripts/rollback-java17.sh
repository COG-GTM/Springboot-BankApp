#!/bin/bash
# Rollback script to revert to Java 17
git checkout pom.xml
git checkout Dockerfile
echo "Rolled back to Java 17 configuration"
