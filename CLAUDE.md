# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

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

## Tech Stack

- Java 21
- Spring Boot 3.5.x with Web, JPA, Validation, Actuator
- MySQL (via Docker Compose with spring-boot-docker-compose for local dev)
- Lombok
- SpringDoc OpenAPI (Swagger UI)

## Architecture Overview

This is a DDD-based monolithic Spring Boot application following domain-centric package structure with layer responsibility separation.

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed guide.

### Package Structure

```
com.examples.springbootmonolithicstarter
├── global/                    # Cross-cutting concerns
│   ├── config/                # Global configs (JPA, Security, Web, Kafka)
│   ├── response/              # API response standards (ApiResponse, ErrorResponse)
│   ├── exception/             # Global exception handling (GlobalExceptionHandler, ErrorCode)
│   └── util/                  # Utilities
│
└── domains/                   # Domain modules
    ├── common/                # Shared domain infrastructure (outbox, saga)
    └── {domain-name}/         # e.g., order, product, customer
        ├── controller/        # REST controllers
        ├── service/
        │   ├── application/   # Application Service (use-case orchestration)
        │   └── domain/        # Domain Service (business rules)
        ├── repository/        # JPA repositories
        ├── model/             # Entity, Value Object, Enum
        ├── dto/
        │   ├── request/       # Request DTOs
        │   └── response/      # Response DTOs
        ├── exception/         # Domain exceptions
        ├── event/             # Domain events (optional)
        ├── saga/              # Saga implementation (optional)
        └── kafka/             # Kafka Producer/Consumer (optional)
```

### Layer Rules

| Layer | Can Reference | Cannot Reference |
|-------|--------------|------------------|
| Controller | Application Service only | Domain Service, Repository, Model |
| Application Service | Own/other domain's Repository, Domain Service | Other domain's Application Service |
| Domain Service | Own domain's Repository, Model | Other domain's Domain Service |
| Model | Value Object, Enum | Repository, Service |

### Key Design Principles

**Controller**: HTTP handling, request validation (@Valid), calls Application Service only - no business logic

**Application Service**: Use-case flow management, transaction boundaries, orchestrates Repository/Domain Service

**Domain Service**: Business rules that don't fit in a single Entity, stateless, own domain only, no transaction management

**Entity**:
- `protected` default constructor with `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- Static factory methods for object creation
- Getter only, no Setter
- Business methods for state changes (e.g., `cancel()`, `confirm()`, `ship()`)

**Value Object**:
- Static factory methods, validation on creation
- Immutable (final fields), override `equals/hashCode`
- Use `@Embeddable`

**DTO**: Request/Response separated, Response uses `from()` factory method

### Naming Conventions

#### Class Names

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `{Domain}Controller` | OrderController |
| Application Service | `{Domain}ApplicationService` | OrderApplicationService |
| Domain Service | `{Concept}Service` | OrderPricingService, StockService |
| Entity | Noun | Order, Product |
| Value Object | Noun | Money, Address |
| Repository | `{Entity}Repository` | OrderRepository |
| Request DTO | `{Verb}{Domain}Request` | CreateOrderRequest |
| Response DTO | `{Domain}Response` | OrderResponse |
| Exception | `{Domain}{Reason}Exception` | OrderNotFoundException |

#### Method Names

| Type | Pattern | Example |
|------|---------|---------|
| Application Service | Use-case verb | createOrder, cancelOrder |
| Domain Service | Domain rule verb | calculateTotalPrice, reserveStock |
| Entity | Business action verb | cancel, confirm, ship |
| Repository | find, save, delete | findByCustomerId, save |

## Testing Strategy

See [TESTING.md](docs/TESTING.md) for detailed testing guide.

| Layer | Test Type | Annotation | Speed |
|-------|-----------|------------|-------|
| Model (Entity, VO) | Unit Test | None | Fast |
| Domain Service | Unit Test | None | Fast |
| Application Service | Unit Test | `@ExtendWith(MockitoExtension.class)` | Fast |
| Repository | Slice Test | `@DataJpaTest` | Medium |
| Controller | Slice Test | `@WebMvcTest` | Medium |

### Test Patterns
- **BDD Style**: given-when-then structure
- **@Nested**: Group related tests
- **@DisplayName**: Korean test names for readability
- **Factory Methods**: Reusable test data creation