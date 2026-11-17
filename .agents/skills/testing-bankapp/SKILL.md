# Testing the Springboot BankApp

## Prerequisites
- Docker and docker-compose installed
- Java 21 JDK (for local Maven builds)

## Devin Secrets Needed
- None required for local testing. The app uses hardcoded local dev credentials defined in `application.properties` and `docker-compose.yml`.

## Building the Docker Image
```bash
docker build -t bankapp:java21 .
```

## Running the Full Stack
The docker-compose.yml uses env vars `DUSER` and `IMAGE` for the app image. To run with a locally built image:
```bash
DUSER=library IMAGE=bankapp:java21 docker compose up -d
```
This starts:
- MySQL container (port 3306 internal, credentials in `docker-compose.yml`)
- BankApp container (port 8080)

Wait for both containers to be healthy before testing:
```bash
docker ps  # Check STATUS column shows "healthy"
```

## Verifying Java Version Inside Container
```bash
docker run --rm --entrypoint java bankapp:java21 -version
```

## Key Endpoints
- `/login` — Login page (unauthenticated)
- `/register` — Registration page (unauthenticated)
- `/dashboard` — Main dashboard (authenticated)
- `/transactions` — Transaction history (authenticated)

## Test Flow
1. Navigate to `http://localhost:8080/register`
2. Register a new user (any username/password)
3. Login at `/login` with the registered credentials
4. Dashboard shows welcome message and $0.00 balance
5. Click Deposit, enter an amount, submit — balance updates
6. Click Transactions nav link to verify transaction history

## Local Maven Build
Requires Java 21:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn clean install -DskipTests  # No MySQL needed for compilation
mvn clean install              # Requires running MySQL on localhost:3306
```

## Notes
- Default branch is `DevOps` (not main/master)
- The `application.properties` uses `bankappdb` as DB name but docker-compose overrides to `BankDB` via env vars
- Docker build may emit warnings about deprecated `MAINTAINER` and inconsistent `as`/`FROM` casing — these are pre-existing
- The app uses Spring Security — all endpoints except `/login` and `/register` require authentication
