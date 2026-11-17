# Example API Microservice

A reference/template Spring Boot microservice demonstrating a clean REST API implementation. Use this as a starting point when building new APIs in this ecosystem.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4.x**
- **Spring Data JPA** (MySQL)
- **Spring Boot Actuator** (health, info, prometheus)
- **Lombok**
- **Maven**
- **Docker** (multi-stage build with non-root user)

## Build & Run Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL running on `localhost:3306` with a database named `ExampleDB`

### Run with Maven

```bash
cd example-api
mvn spring-boot:run
```

The application starts on **port 8081**.

### Run with Docker

```bash
cd example-api
docker build -t example-api .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/ExampleDB?useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=UTC \
  -e MYSQL_PASSWORD=Test@123 \
  example-api
```

### Run with Docker Compose (from repo root)

```bash
docker-compose up -d
```

This starts MySQL, the main BankApp, and the Example API together.

## API Endpoints

| Method   | Endpoint          | Description         |
|----------|-------------------|---------------------|
| `GET`    | `/api/items`      | List all items      |
| `GET`    | `/api/items/{id}` | Get item by ID      |
| `POST`   | `/api/items`      | Create a new item   |
| `PUT`    | `/api/items/{id}` | Update an item      |
| `DELETE` | `/api/items/{id}` | Delete an item      |
| `GET`    | `/api/health`     | Custom health check |
| `GET`    | `/actuator/health`| Actuator health     |

### Example curl Commands

**List all items:**
```bash
curl http://localhost:8081/api/items
```

**Create an item:**
```bash
curl -X POST http://localhost:8081/api/items \
  -H "Content-Type: application/json" \
  -d '{"name": "Widget", "description": "A sample widget"}'
```

**Get an item by ID:**
```bash
curl http://localhost:8081/api/items/1
```

**Update an item:**
```bash
curl -X PUT http://localhost:8081/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Widget", "description": "An updated widget"}'
```

**Delete an item:**
```bash
curl -X DELETE http://localhost:8081/api/items/1
```

**Health check:**
```bash
curl http://localhost:8081/api/health
```

## Using as a Template

To create a new microservice based on this template:

1. Copy the `example-api/` directory to a new directory (e.g., `my-new-api/`)
2. Update `pom.xml`: change `artifactId`, `name`, and `description`
3. Rename the Java package from `com.example.exampleapi` to your package
4. Replace the `Item` model with your domain entity
5. Update `application.properties` with your database name and port
6. Update the `Dockerfile` to expose the correct port
7. Add your new service to the root `docker-compose.yml`
