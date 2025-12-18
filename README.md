# Springboot-BankApp

A multi-tier banking web application built with Spring Boot, featuring user authentication, account management, and transaction processing capabilities.

![Login Page](images/login.png)
![Transactions Page](images/transactions.png)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Docker Deployment](#docker-deployment)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Security](#security)
- [DevOps and CI/CD](#devops-and-cicd)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Monitoring](#monitoring)
- [Contributing](#contributing)

## Overview

Springboot-BankApp is a full-featured banking application that demonstrates modern Java development practices using Spring Boot 3.3.3. The application provides essential banking functionalities including user registration, secure authentication, deposits, withdrawals, and fund transfers between accounts.

## Features

The application provides the following core banking features:

**User Management** allows new users to register accounts with secure password encryption using BCrypt. Users can log in and log out securely with session management handled by Spring Security.

**Account Operations** include viewing account dashboard with current balance, depositing funds into accounts, withdrawing funds with insufficient balance validation, and transferring money between user accounts.

**Transaction History** provides a complete record of all account transactions including deposits, withdrawals, and transfers with timestamps.

## Technology Stack

The application is built using the following technologies:

**Backend Framework**: Spring Boot 3.3.3 with Spring MVC for web handling, Spring Data JPA for database operations, and Spring Security for authentication and authorization.

**Database**: MySQL 8.0 for persistent data storage with Hibernate ORM for object-relational mapping.

**Frontend**: Thymeleaf templating engine with Thymeleaf Spring Security integration for secure view rendering.

**Build Tool**: Apache Maven for dependency management and build automation.

**Containerization**: Docker with multi-stage builds for optimized container images.

**Orchestration**: Kubernetes manifests and Helm charts for cloud-native deployment.

## Architecture

The application follows a layered architecture pattern:

```
+-----------------------------------------------------------+
|                    Presentation Layer                      |
|              (Thymeleaf Templates + CSS)                   |
+-----------------------------------------------------------+
|                    Controller Layer                        |
|                   (BankController)                         |
+-----------------------------------------------------------+
|                     Service Layer                          |
|                   (AccountService)                         |
+-----------------------------------------------------------+
|                   Repository Layer                         |
|        (AccountRepository, TransactionRepository)          |
+-----------------------------------------------------------+
|                     Data Layer                             |
|                   (MySQL Database)                         |
+-----------------------------------------------------------+
```

**Model Classes**: `Account` (implements UserDetails for Spring Security integration) and `Transaction` entities represent the core domain objects.

**Repository Interfaces**: JPA repositories provide database access with custom query methods for finding accounts by username and transactions by account ID.

**Service Layer**: `AccountService` implements `UserDetailsService` for authentication and contains all business logic for banking operations.

**Controller**: `BankController` handles HTTP requests and maps them to appropriate service methods.

## Getting Started

### Prerequisites

Before running the application, ensure you have the following installed:

- Java 17 or higher (JDK)
- Apache Maven 3.8+
- MySQL 8.0+
- Docker and Docker Compose (for containerized deployment)

### Local Development

1. **Clone the repository**:
   ```bash
   git clone https://github.com/COG-GTM/Springboot-BankApp.git
   cd Springboot-BankApp
   ```

2. **Set up MySQL database**:
   ```bash
   # Start MySQL service
   sudo systemctl start mysql
   
   # Create the database
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bankappdb;"
   ```

3. **Configure database connection** (if needed):
   
   Edit `src/main/resources/application.properties` to match your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/bankappdb?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

4. **Build the application**:
   ```bash
   mvn clean install
   ```

5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**:
   
   Open your browser and navigate to `http://localhost:8080`

### Docker Deployment

1. **Build the Docker image**:
   ```bash
   docker build -t bankapp:latest .
   ```

2. **Run with Docker Compose**:
   ```bash
   # Set environment variables
   export DUSER=your-dockerhub-username
   export IMAGE=bankapp:latest
   
   # Start the application stack
   docker-compose up -d
   ```

   This will start both the MySQL database and the BankApp application. The application will be available at `http://localhost:8080`.

3. **Stop the application**:
   ```bash
   docker-compose down
   ```

## API Endpoints

The application exposes the following web endpoints:

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/login` | Display login page | Public |
| POST | `/login` | Process login | Public |
| GET | `/register` | Display registration form | Public |
| POST | `/register` | Create new account | Public |
| GET | `/dashboard` | View account dashboard | Required |
| POST | `/deposit` | Deposit funds | Required |
| POST | `/withdraw` | Withdraw funds | Required |
| POST | `/transfer` | Transfer to another account | Required |
| GET | `/transactions` | View transaction history | Required |
| GET | `/logout` | Log out user | Required |

## Database Schema

The application uses two main database tables:

**Account Table**:
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key, auto-generated |
| username | VARCHAR | Unique username |
| password | VARCHAR | BCrypt encrypted password |
| balance | DECIMAL | Current account balance |

**Transaction Table**:
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key, auto-generated |
| amount | DECIMAL | Transaction amount |
| type | VARCHAR | Transaction type (Deposit, Withdrawal, Transfer) |
| timestamp | DATETIME | Transaction timestamp |
| account_id | BIGINT | Foreign key to Account |

## Security

The application implements several security measures:

**Authentication**: Spring Security handles user authentication with form-based login. Passwords are encrypted using BCrypt before storage.

**Authorization**: All endpoints except `/register` and `/login` require authentication. The security configuration uses Spring Security 6 with the new lambda DSL.

**Session Management**: Sessions are properly invalidated on logout with authentication cleared.

**CSRF Protection**: Currently disabled for simplicity but can be enabled for production deployments.

## DevOps and CI/CD

The project includes comprehensive DevOps tooling:

**Jenkins Pipeline** (`Jenkinsfile`): Implements a complete CI pipeline with the following stages:
- Workspace cleanup
- Git code checkout
- Trivy filesystem security scan
- OWASP dependency vulnerability check
- SonarQube code analysis and quality gates
- Docker image build and push to DockerHub
- Automatic trigger of CD pipeline on success

**Shared Library**: The pipeline uses a shared Jenkins library for reusable pipeline functions.

**Security Scanning**: Integration with Trivy for container security and OWASP for dependency vulnerability scanning.

**Code Quality**: SonarQube integration for static code analysis and quality gate enforcement.

## Kubernetes Deployment

The `kubernetes/` directory contains manifests for deploying to Kubernetes:

- `bankapp-namespace.yaml` - Dedicated namespace for the application
- `bankapp-deployment.yml` - Application deployment configuration
- `bankapp-service.yaml` - Service exposure configuration
- `bankapp-ingress.yml` - Ingress rules for external access
- `bankapp-hpa.yml` - Horizontal Pod Autoscaler configuration
- `mysql-deployment.yml` - MySQL database deployment
- `mysql-service.yaml` - MySQL service configuration
- `persistent-volume.yaml` and `persistent-volume-claim.yaml` - Storage configuration
- `configmap.yaml` and `secrets.yaml` - Configuration and secrets management
- `letsencrypt-clusterissuer.yaml` - TLS certificate management

**Helm Charts**: The `helm/bankapp/` directory contains Helm charts for templated Kubernetes deployments.

**GitOps**: The `GitOps/` directory contains the CD Jenkinsfile for ArgoCD-based continuous deployment.

For detailed Kubernetes deployment instructions, see [README-K8S.md](README-K8S.md).

## Monitoring

The application supports monitoring through Prometheus and Grafana:

1. **Install Prometheus Stack**:
   ```bash
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   kubectl create namespace prometheus
   helm install stable prometheus-community/kube-prometheus-stack -n prometheus
   ```

2. **Access Grafana Dashboard**:
   ```bash
   # Get Grafana password
   kubectl get secret --namespace prometheus stable-grafana -o jsonpath="{.data.admin-password}" | base64 --decode
   ```

3. **Default Credentials**: Username: `admin`, Password: retrieved from secret

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please ensure your code follows the existing code style and includes appropriate tests.

## License

This project is open source and available under the MIT License.
