# PES Expo Microservices

A Spring Boot microservices project with SonarQube integration for code quality analysis.

## Project Structure

```
rootProject/
├── build.gradle                 # Root build configuration with SonarQube plugin
├── settings.gradle              # Multi-module project settings
├── docker-compose.sonarqube.yml # SonarQube server setup
├── src/                         # Root application
├── product-service/             # Product microservice (port 9002)
│   ├── build.gradle
│   └── src/
│       ├── main/java/.../
│       │   ├── controller/ProductController.java
│       │   ├── service/ProductService.java
│       │   ├── service/impl/ProductServiceImpl.java
│       │   ├── repository/ProductRepository.java
│       │   ├── domain/Product.java
│       │   └── client/OrderClient.java
│       └── test/java/.../
│           └── service/impl/ProductServiceImplTest.java
└── order-service/               # Order microservice (port 9003)
    ├── build.gradle
    └── src/
        ├── main/java/.../
        │   ├── controller/OrderController.java
        │   ├── service/OrderService.java
        │   ├── service/impl/OrderServiceImpl.java
        │   ├── repository/OrderRepository.java
        │   ├── domain/Order.java
        │   └── client/ProductClient.java
        └── test/java/.../
            └── service/impl/OrderServiceImplTest.java
```

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21      | Programming language |
| Spring Boot | 4.0.1   | Application framework |
| Gradle | 9.2     | Build tool |
| PostgreSQL | 16      | Database |
| JaCoCo | 0.8.12  | Code coverage |
| SonarQube | LTS     | Code quality analysis |

## Prerequisites

- Java 21+
- Docker & Docker Compose
- PostgreSQL databases running (or use Docker)

## Quick Start

### 1. Start SonarQube Server

```bash
docker compose -f docker-compose.sonarqube.yml up -d
```

Wait for SonarQube to start (check logs):
```bash
docker logs -f sonarqube
```

Access SonarQube at: http://localhost:9000
- Default credentials: `admin` / `admin`

### 2. Generate SonarQube Token

1. Login to http://localhost:9000
2. Click profile icon (top-right) → **My Account**
3. Go to **Security** tab
4. Generate a new token:
   - Name: `gradle-analysis`
   - Type: `Project Analysis Token`
5. Copy the token (shown only once)

### 3. Run Tests with Coverage

```bash
./gradlew clean test
```

### 4. Run SonarQube Analysis

```bash
export SONAR_TOKEN=your_token_here
./gradlew sonar
```

Or in one command:
```bash
SONAR_TOKEN=your_token_here ./gradlew test sonar
```

### 5. View Results

Open http://localhost:9000 and click on **"PES Expo Microservices"** project.

---

## Configuration Details

### Gradle Build Configuration

#### Root `build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'org.sonarqube' version '6.0.1.5171'
    id 'jacoco'
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.12"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true   // Required for SonarQube
        html.required = true  // Human-readable report
    }
}

// SonarQube configuration
sonar {
    properties {
        property "sonar.projectKey", "pes-expo-microservices"
        property "sonar.projectName", "PES Expo Microservices"
        property "sonar.host.url", "http://localhost:9000"
        property "sonar.sourceEncoding", "UTF-8"
        property "sonar.java.source", "21"
        property "sonar.token", System.getenv("SONAR_TOKEN") ?: ""
    }
}
```

#### Subproject `build.gradle` (product-service, order-service)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'jacoco'
}

tasks.named('test') {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

jacoco {
    toolVersion = "0.8.12"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

### Docker Compose Configuration

#### `docker-compose.sonarqube.yml`

```yaml
services:
  sonarqube:
    image: sonarqube:lts-community
    container_name: sonarqube
    depends_on:
      - sonarqube-db
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://sonarqube-db:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_logs:/opt/sonarqube/logs
    ports:
      - "9000:9000"

  sonarqube-db:
    image: postgres:16-alpine
    container_name: sonarqube-db
    environment:
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar
      POSTGRES_DB: sonar
    volumes:
      - postgresql_data:/var/lib/postgresql/data
```

---

## Flow Process

### Code Quality Analysis Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DEVELOPMENT WORKFLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │  Developer   │
    │  writes code │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  Run Tests   │  ./gradlew test
    │  + Coverage  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │   JaCoCo     │  Generates coverage reports
    │   Reports    │  - XML (for SonarQube)
    │              │  - HTML (for developers)
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  SonarQube   │  ./gradlew sonar
    │   Analysis   │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  SonarQube   │  http://localhost:9000
    │   Dashboard  │
    └──────────────┘
```

### Test Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              TEST FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────┘

./gradlew test
      │
      ├──► :test (root project)
      │         │
      │         └──► :jacocoTestReport
      │
      ├──► :product-service:test
      │         │
      │         ├── ProductServiceImplTest (14 tests)
      │         │     ├── createProduct tests
      │         │     ├── findById tests
      │         │     ├── findAll tests
      │         │     ├── updateProduct tests
      │         │     ├── deleteProduct tests
      │         │     └── findProductWithOrders tests
      │         │
      │         └──► :product-service:jacocoTestReport
      │
      └──► :order-service:test
                │
                ├── OrderServiceImplTest (11 tests)
                │     ├── createOrder tests
                │     ├── findAll tests
                │     ├── findByUuid tests
                │     ├── findByProductUuid tests
                │     └── deleteOrder tests
                │
                └──► :order-service:jacocoTestReport
```

### SonarQube Analysis Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SONARQUBE ANALYSIS                                  │
└─────────────────────────────────────────────────────────────────────────────┘

./gradlew sonar
      │
      ▼
┌─────────────────┐
│  Collect Data   │
│                 │
│  • Source files │  src/main/java/**/*.java
│  • Test files   │  src/test/java/**/*.java
│  • Coverage     │  build/reports/jacoco/test/jacocoTestReport.xml
│  • Binaries     │  build/classes/java/main
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Send to        │
│  SonarQube      │───────► http://localhost:9000
│  Server         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Analysis       │
│  Results        │
│                 │
│  • Bugs         │  Code defects
│  • Vulnerabilities │  Security issues
│  • Code Smells  │  Maintainability issues
│  • Coverage %   │  Test coverage percentage
│  • Duplications │  Duplicate code blocks
└─────────────────┘
```

---

## Microservices Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MICROSERVICES ARCHITECTURE                           │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │     Client      │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    │                                      │
                    ▼                                      ▼
         ┌──────────────────┐                   ┌──────────────────┐
         │  Product Service │                   │   Order Service  │
         │    Port: 9002    │◄─────────────────►│    Port: 9003    │
         └────────┬─────────┘   REST Clients    └────────┬─────────┘
                  │                                       │
                  ▼                                       ▼
         ┌──────────────────┐                   ┌──────────────────┐
         │   PostgreSQL     │                   │   PostgreSQL     │
         │   product_db     │                   │    order_db      │
         │   Port: 5991     │                   │   Port: 5992     │
         └──────────────────┘                   └──────────────────┘
```

### API Endpoints

#### Product Service (http://localhost:9002)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/products` | Create a product |
| GET | `/api/v1/products` | Get all products |
| GET | `/api/v1/products/{uuid}` | Get product by UUID |
| PUT | `/api/v1/products/{uuid}` | Update product |
| DELETE | `/api/v1/products/{uuid}` | Delete product |
| GET | `/api/v1/products/{uuid}/orders` | Get product with orders |

#### Order Service (http://localhost:9003)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create an order |
| GET | `/api/v1/orders` | Get all orders |
| GET | `/api/v1/orders/{uuid}` | Get order by UUID |
| GET | `/api/v1/orders/product/{productUuid}` | Get orders by product |
| DELETE | `/api/v1/orders/{uuid}` | Delete order |

---

## Reports Location

After running tests, reports are available at:

| Report | Location |
|--------|----------|
| JaCoCo HTML (root) | `build/reports/jacoco/test/html/index.html` |
| JaCoCo HTML (product-service) | `product-service/build/reports/jacoco/test/html/index.html` |
| JaCoCo HTML (order-service) | `order-service/build/reports/jacoco/test/html/index.html` |
| JaCoCo XML (for SonarQube) | `*/build/reports/jacoco/test/jacocoTestReport.xml` |
| Test Results | `*/build/reports/tests/test/index.html` |

---

## Common Commands

```bash
# Start SonarQube
docker compose -f docker-compose.sonarqube.yml up -d

# Stop SonarQube
docker compose -f docker-compose.sonarqube.yml down

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :product-service:test
./gradlew :order-service:test

# Generate coverage report only
./gradlew jacocoTestReport

# Run SonarQube analysis
SONAR_TOKEN=your_token ./gradlew sonar

# Full pipeline: clean, test, and analyze
SONAR_TOKEN=your_token ./gradlew clean test sonar

# View Gradle tasks
./gradlew tasks --group=verification
```

---

## Troubleshooting

### SonarQube Authentication Error

If you see "Not authorized" error:
1. Verify your token is valid at http://localhost:9000
2. Use environment variable: `export SONAR_TOKEN=your_token`
3. Run: `./gradlew sonar`

### SonarQube Not Starting

Check if ports are available:
```bash
lsof -i :9000
```

Check container logs:
```bash
docker logs sonarqube
```

### Low Code Coverage

Ensure:
1. JaCoCo plugin is added to each module's `build.gradle`
2. Tests are actually running: `./gradlew test --info`
3. XML reports are generated: check `build/reports/jacoco/test/jacocoTestReport.xml`

### Gradle Version Issues

This project requires Gradle 8.14+ for Spring Boot 4.0.1 compatibility.
Check version:
```bash
./gradlew --version
```
