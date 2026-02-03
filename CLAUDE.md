# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Reference

### Build and Run Commands

```bash
# Build
./gradlew build

# Run application (starts MySQL via Docker Compose automatically)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.examples.springbootmonolithicstarter.SomeTestClass"

# Run a single test method
./gradlew test --tests "com.examples.springbootmonolithicstarter.SomeTestClass.testMethodName"

# Clean build
./gradlew clean build
```

### Tech Stack

- Java 21
- Spring Boot 3.5.x with Web, JPA, Validation, Actuator
- MySQL (via Docker Compose with spring-boot-docker-compose for local dev)
- Lombok
- SpringDoc OpenAPI (Swagger UI)

## Documentation

This is a DDD-based monolithic Spring Boot application. Detailed documentation is available in the `docs/` folder:

### 📚 Architecture & Design

**[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Complete architecture guide
- Package structure and domain-centric organization
- Layer responsibilities and design principles
- Entity, Value Object, Repository patterns
- DTO and Exception handling
- Naming conventions (classes and methods)
- Code examples for each layer

**Key Highlights:**
| Layer | Responsibility | Can Reference |
|-------|-----------------|---------------|
| **Controller** | HTTP handling, validation | Application Service only |
| **Application Service** | Use-case orchestration | Repository, Domain Service |
| **Domain Service** | Business rules | Repository, Model |
| **Model** (Entity/VO) | Core business logic | Value Objects, Enums |

### 🧪 Testing & Quality

**[TESTING.md](docs/TESTING.md)** - Comprehensive testing strategy
- Test pyramid and layer-by-layer testing approach
- Unit tests: Model, Domain Service, Application Service
- Integration tests: Repository, Controller
- Test patterns: BDD style, @Nested, @DisplayName
- Complete code examples for each test type
- Running specific tests and viewing reports

**Quick Test Commands:**
```bash
./gradlew test --tests "*.ProductTest"
./gradlew test --tests "*.ProductTest\$StockManagement"
open build/reports/tests/test/index.html
```

### 📝 Commits & Versioning

**[COMMITS.md](docs/COMMITS.md)** - Conventional Commits guide
- Commit message structure and types (feat, fix, docs, refactor, etc.)
- Scope and breaking changes
- Examples for different commit scenarios
- Integration with version management and changelog generation

**Commit Format:**
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Examples:
- `feat(auth): JWT token expiration handling`
- `fix(api): Handle 404 when search returns no results`
- `docs: Update README installation guide`