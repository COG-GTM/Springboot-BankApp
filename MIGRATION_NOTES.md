# Java 8 to Java 11 Migration Notes

This document summarizes the changes made to migrate the Springboot-BankApp from Java 8 to Java 11.

## Build Configuration Changes

### Maven Compiler Plugin
The maven-compiler-plugin was updated from version 3.8.0 to 3.11.0 with the following configuration changes:

- Replaced `<source>1.8</source>` and `<target>1.8</target>` with `<release>11</release>`
- The `release` flag ensures both source compatibility and bytecode target are set consistently

### Java Version Properties
Updated properties in pom.xml:
- `java.version`: Changed from 17 to 11
- Added `maven.compiler.release`: Set to 11
- Added `project.build.sourceEncoding`: Set to UTF-8

### New Maven Plugins Added
The following plugins were added to ensure Java 11 compatibility and improve build quality:

- **maven-surefire-plugin** (3.2.5): For running unit tests with Java 11 support
- **maven-failsafe-plugin** (3.2.5): For running integration tests
- **maven-enforcer-plugin** (3.5.0): Enforces minimum Java version requirement of 11

## Dependency Updates

### MySQL Connector
Updated the MySQL connector artifact coordinates to the new reverse-DNS compliant Maven 2+ coordinates:
- Old: `mysql:mysql-connector-java:8.0.33`
- New: `com.mysql:mysql-connector-j:8.0.33`

### Test Dependencies
Added H2 database dependency for testing:
- `com.h2database:h2` (test scope)

This allows tests to run without requiring a MySQL database connection.

## Test Configuration

### Test Application Properties
Created `src/test/resources/application.properties` with H2 database configuration for testing:
- Uses in-memory H2 database (`jdbc:h2:mem:testdb`)
- Configured with H2Dialect for Hibernate
- Uses `create-drop` DDL auto mode for clean test isolation

## Removed/Deprecated JDK Modules

No JAXB, JAX-WS, CORBA, or JavaFX dependencies were found in this project. The application does not use any modules that were removed in Java 11.

## Illegal Reflective Access

No illegal reflective access warnings were observed during the build. The application and its dependencies are compatible with Java 11's module system.

## Known Issues and Follow-ups

### JPA Open-in-View Warning
The following warning appears during tests:
```
spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering.
```
This is informational and can be addressed by explicitly setting `spring.jpa.open-in-view=false` in application.properties if desired.

### JVM Sharing Warning
The following warning appears during tests:
```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```
This is a normal JVM warning when running with certain class loading configurations and does not affect functionality.

## Validation

- Build passes with `mvn clean verify`
- All tests pass on JDK 11+
- No illegal reflective access warnings
- Maven enforcer plugin ensures Java 11+ is required

## Compatibility

This migration targets Java 11 as the minimum version. The application will also run on later LTS versions (Java 17, Java 21) due to backward compatibility.
