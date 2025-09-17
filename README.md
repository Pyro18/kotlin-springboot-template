# 🚀 Kotlin Spring Boot Production Template

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.25-blue.svg)](http://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.3.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI/CD](https://github.com/yourusername/spring-app/actions/workflows/ci.yml/badge.svg)](https://github.com/Pyro18/kotlin-springboot-template/actions)

Production-ready template for Kotlin + Spring Boot applications with all modern best practices.

## 🎯 Features

### Core

* ✅ **Kotlin 1.9.25** + **Spring Boot 3.3.4** with Java 21
* ✅ **Gradle Version Catalog** for centralized dependency management
* ✅ **Multi-profile configuration** (dev, test, prod)
* ✅ **Full CRUD** with User entity example
* ✅ **PostgreSQL** + **Flyway** for migrations
* ✅ **Redis** for distributed caching
* ✅ **MapStruct** for DTO mapping
* ✅ **Arrow-kt** for functional programming

### Security & API

* 🔐 **Spring Security** with JWT authentication
* 📝 **OpenAPI 3.0** (Swagger) documentation
* 🚦 **Configurable rate limiting**
* 🔄 **CORS** configuration
* 🛡️ **Input validation** with Bean Validation

### DevOps & Monitoring

* 🐳 **Docker** optimized multi-stage build
* ☸️ **Kubernetes** production-ready manifests
* 📊 **Prometheus** + **Grafana** for metrics
* 📚 **ELK Stack** (Elasticsearch, Logstash, Kibana) for logging
* 🏥 **Health checks** (liveness, readiness, startup probes)
* 📈 **Micrometer** metrics with custom metrics

### Testing

* 🧪 **Kotest** + **MockK** for unit testing
* 🔧 **Testcontainers** for integration testing
* 📊 **Code coverage** with JaCoCo
* 🎯 **API testing** with RestAssured

### Code Quality

* 🎨 **Ktlint** for code formatting
* 🔍 **Detekt** for static analysis
* 🚀 **GitHub Actions** CI/CD pipeline
* 📦 **Jib** for building/pushing Docker images without Docker daemon

## 📁 Project Structure

```
kotlin-springboot-template/
├── gradle/                 # Gradle wrapper and version catalog
│   └── libs.versions.toml  # Centralized version management
├── src/
│   ├── main/
│   │   ├── kotlin/        # Source code
│   │   └── resources/     # Configurations and resources
│   └── test/              # Test code
├── docker/                # Docker configurations
├── k8s/                   # Kubernetes manifests
├── .github/workflows/     # CI/CD pipelines
└── Makefile               # Utility commands
```

## 🚀 Quick Start

### Prerequisites

* Java 21
* Docker & Docker Compose
* Kubernetes (optional)
* Make (optional but recommended)

### Local Setup

1. **Clone the repository**

```bash
git clone https://github.com/Pyro18/kotlin-springboot-template.git
cd kotlin-springboot-template
```

2. **Set up environment**

```bash
make env-setup
# Edit .env with your configurations
```

3. **Start services**

```bash
# Start database and Redis
make docker-run-dev

# Run migrations
make db-migrate

# Start application
make run-dev
```

4. **Access services**

* API: [http://localhost:8080](http://localhost:8080)
* Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* Health: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
* Prometheus: [http://localhost:9090](http://localhost:9090)
* Grafana: [http://localhost:3000](http://localhost:3000) (admin/admin)
* Kibana: [http://localhost:5601](http://localhost:5601)

### Docker Development

```bash
# Build local image
make docker-build-local

# Run with full docker-compose
make docker-run

# View logs
make docker-logs

# Stop everything
make docker-stop
```

## 🧪 Testing

```bash
# Run all tests
make test

# Unit tests only
./gradlew test

# Integration tests with Testcontainers
make test-integration

# Coverage report
./gradlew jacocoTestReport
```

## 📦 Build & Deploy

### Production Build

```bash
# Build JAR
make build

# Build and push Docker image (uses Jib, no Docker daemon required)
make docker-build
```

### Deploy to Kubernetes

```bash
# Full deploy
make k8s-deploy

# Check status
make k8s-status

# View logs
make k8s-logs

# Scale deployment
make k8s-scale REPLICAS=5

# Rollback if needed
make k8s-rollback
```

## 🔧 Configuration

### Available Profiles

* **dev**: Local development with hot-reload
* **test**: For automated test execution
* **prod**: Production-ready configuration
* **docker**: For running in containers
* **k8s**: For Kubernetes deployment

### Main Environment Variables

```env
# Database
DB_USERNAME=appuser
DB_PASSWORD=secure_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appdb

# Redis
REDIS_PASSWORD=redis_password

# JWT
JWT_SECRET=your-256-bit-secret

# CORS
CORS_ORIGINS=http://localhost:3000,https://yourdomain.com

# File Upload
FILE_STORAGE_PATH=/app/uploads
```

## 📊 Monitoring

### Prometheus Metrics

Available at `/actuator/prometheus`:

* JVM metrics (memory, GC, threads)
* HTTP metrics (request count, latency)
* Database connection pool
* Cache statistics
* Custom business metrics

### Health Checks

* `/actuator/health/liveness` - Kubernetes liveness probe
* `/actuator/health/readiness` - Kubernetes readiness probe
* `/actuator/health` - Full details

### Logging

Configured with Logstash JSON encoder for ELK stack.
MDC pattern for correlation ID across microservices.

## 🛡️ Security

### Implemented

* JWT authentication/authorization
* CORS protection
* Rate limiting
* Input validation
* SQL injection protection (JPA)
* XSS protection
* CSRF protection
* Secure headers

### Best Practices

* Non-root Docker user
* Read-only root filesystem in K8s
* Secrets management via K8s secrets
* Network policies
* Pod security policies

## 🔄 API Endpoints

### User Management

| Method | Endpoint                           | Description     | Auth Required    |
| ------ | ---------------------------------- | --------------- | ---------------- |
| GET    | `/api/v1/users`                    | List all users  | ADMIN, MODERATOR |
| GET    | `/api/v1/users/{id}`               | Get user by ID  | ADMIN or OWNER   |
| POST   | `/api/v1/users`                    | Create new user | PUBLIC           |
| PUT    | `/api/v1/users/{id}`               | Update user     | ADMIN or OWNER   |
| DELETE | `/api/v1/users/{id}`               | Delete user     | ADMIN            |
| POST   | `/api/v1/users/{id}/upload-avatar` | Upload avatar   | OWNER            |

## 📝 Useful Commands

```bash
# Development
make run-dev       # Run in development mode
make test          # Run tests
make lint          # Check code quality
make format        # Format code

# Database
make db-migrate    # Run migrations
make db-info       # Migration info
make db-clean      # Clean database

# Docker
make docker-run    # Run full stack
make docker-logs   # View logs
make docker-clean  # Remove everything

# Kubernetes
make k8s-deploy    # Deploy to K8s
make k8s-status    # Check status
make k8s-scale     # Scale pods

# Utility
make help          # Show all commands
make fresh-start   # Clean start dev environment
```

## 🤝 Contributing

1. Fork the project
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

* Follow Kotlin coding conventions
* Run `make format` before committing
* Write tests for new features
* Update documentation

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 🙏 Acknowledgments

* [Spring Boot](https://spring.io/projects/spring-boot)
* [Kotlin](https://kotlinlang.org/)
* [Testcontainers](https://www.testcontainers.org/)
* [Arrow-kt](https://arrow-kt.io/)

## 📞 Support

For issues or questions:

* 🐛 Issues: [GitHub Issues](https://github.com/Pyro18/kotlin-springboot-template/issues)

---

Built with ❤️ using Kotlin and Spring Boot
