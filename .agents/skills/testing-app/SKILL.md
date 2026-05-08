# Testing Springboot-BankApp

How to build, run, and test the bank app end-to-end locally.

## Build

```bash
docker build -t bankapp:latest .
```

## Run Full Stack (App + MySQL)

```bash
# Create network
docker network create bankapp-net

# Start MySQL (use credentials from application.properties)
docker run -d --name mysql-test --network bankapp-net \
  -e MYSQL_ROOT_PASSWORD=$DB_PASSWORD \
  -e MYSQL_DATABASE=BankDB \
  -p 3306:3306 mysql:latest

# Wait for MySQL to be healthy (~15-20 seconds)
sleep 20

# Start app (override datasource properties via env vars)
docker run -d --name bankapp-test --network bankapp-net \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-test:3306/BankDB?useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=UTC \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
  -p 8080:8080 bankapp:latest
```

App will be available at http://localhost:8080/login after ~5-10 seconds.

Note: DB credentials are defined in `application.properties` (root user, check file for password). These are local dev-only credentials.

## Core Test Flows

1. **Login page** -- Navigate to http://localhost:8080/login, verify Wells Fargo branding and form renders
2. **Register** -- Click "Register here", create account (any username/password), should redirect to /login
3. **Login** -- Login with registered credentials, should show dashboard with "Welcome, {username}" and $0.00 balance
4. **Deposit** -- Click Deposit, enter amount, submit. Balance should increase.
5. **Withdraw** -- Click Withdraw, enter amount, submit. Balance should decrease.
6. **Transactions** -- Click "Transactions" in nav bar to view transaction history

## Key Routes

- `/login` -- Login page
- `/register` -- Registration page
- `/dashboard` -- Main dashboard (authenticated)
- `/transactions` -- Transaction history (authenticated)

## Cleanup

```bash
docker stop bankapp-test mysql-test
docker rm bankapp-test mysql-test
docker network rm bankapp-net
```

## Notes

- DB credentials are in `application.properties` (local dev only)
- The app uses Spring Security -- all routes except /login and /register require authentication
- Hibernate auto-creates tables (ddl-auto=update)
- The docker-compose.yml in the repo can also be used but requires setting DUSER and IMAGE env vars
