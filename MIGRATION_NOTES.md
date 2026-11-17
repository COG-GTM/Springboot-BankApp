# Java 8 to Java 11 Migration Notes

This document summarizes the changes made to migrate the Springboot-BankApp from Java 8 to Java 11.

## Summary of Changes

### Build Configuration (pom.xml)

The following changes were made to support Java 11:

**Spring Boot Version**: Downgraded from 3.3.3 to 2.7.18 (LTS) because Spring Boot 3.x requires Java 17+, while Spring Boot 2.7.x supports Java 8-17.

**Java Version**: Updated from Java 8 to Java 11 using the `<release>11</release>` configuration in maven-compiler-plugin.

**Maven Plugins Updated**:
- maven-compiler-plugin: 3.11.0 (with `<release>11</release>`)
- maven-surefire-plugin: 3.2.5
- maven-failsafe-plugin: 3.2.5
- maven-enforcer-plugin: 3.5.0 (enforces Java 11+ requirement)

**Dependencies Changed**:
- thymeleaf-extras-springsecurity6 -> thymeleaf-extras-springsecurity5 (Spring Security 5 for Spring Boot 2.7.x)
- Added H2 database for testing (test scope)

### Source Code Changes

**Model Classes (Account.java, Transaction.java)**:
- Changed `jakarta.persistence.*` imports to `javax.persistence.*` (Jakarta EE namespace is used in Spring Boot 3.x, Java EE namespace in Spring Boot 2.7.x)
- Added UserDetails interface methods to Account.java: `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isEnabled()`

**SecurityConfig.java**:
- Updated from Spring Security 6 lambda DSL to Spring Security 5 method chaining style
- Changed `requestMatchers()` to `antMatchers()`
- Changed `authorizeHttpRequests()` to `authorizeRequests()`
- Updated logout, formLogin, and headers configuration to use `.and()` chaining

### Configuration Changes

**application.properties**:
- Changed Hibernate dialect from `MySQL8Dialect` to `MySQL57Dialect` for Hibernate 5 compatibility

**Test Configuration**:
- Added `src/test/resources/application.properties` with H2 in-memory database configuration for testing

### CI/CD

**GitHub Actions**:
- Added `.github/workflows/java11-build.yml` workflow for JDK 11 builds
- Uses Temurin JDK 11 distribution
- Includes Maven caching for faster builds

## Validation

- Build compiles successfully with Java 11
- All tests pass with H2 in-memory database
- Maven enforcer plugin ensures Java 11+ is required

## Known Considerations

1. **Database Compatibility**: The application uses MySQL in production. The test configuration uses H2 for CI/CD compatibility.

2. **Spring Security**: Using Spring Security 5 method chaining style. Some methods are deprecated but functional.

3. **Hibernate Dialect**: Using MySQL57Dialect which is compatible with MySQL 5.7+ and MySQL 8.x.

## Future Considerations

- Consider upgrading to Spring Boot 3.x with Java 17+ for long-term support
- Consider migrating to Jakarta EE namespace when upgrading to Spring Boot 3.x
- Consider using Spring Security 6 lambda DSL when upgrading to Spring Boot 3.x
