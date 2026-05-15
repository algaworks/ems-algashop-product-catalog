# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is the **Product Catalog Service**, a Spring Boot microservice responsible for managing the complete product lifecycle including catalog management, inventory, product images, and pricing. It serves as the primary data source for product information in the Algashop platform.

**Key Responsibilities:**
- Product and category management (create, update, list, retrieve)
- Product inventory/stock management with movement tracking
- Product images upload and storage (S3/LocalStack)
- Pre-signed URLs for secure image access
- Product availability and pricing information
- Domain events publishing for product state changes
- REST API for product queries and information retrieval

**Technology Stack:**
- Java 25, Gradle 9.2.1, Spring Boot 4.0.x
- MongoDB (replica set) for product and inventory data storage
- Spring Security OAuth2 (Resource Server)
- Redis (caching layer)
- AWS S3 integration (LocalStack for local development) for image storage
- Spring Cloud Contract (contract-driven testing)
- TestContainers (integration test databases)
- Domain events pattern for state changes

## Architecture

### Domain-Driven Design with Layered Architecture

The codebase follows a **layered architecture** with strong domain principles:

```
model/             → Domain layer (aggregates, value objects, business logic)
application/       → Application layer (use cases, services, event handlers)
infrastructure/    → Infrastructure layer (persistence, storage, messaging, security)
presentation/      → Presentation layer (REST controllers, DTOs, request/response handling)
```

**Key Layers:**

1. **Domain Layer** (`domain/model/`)
    - **Aggregates:** Core business entities
        - `Product` — Product aggregate root with name, description, price, category, stock information
        - `ProductCategory` — Category for organizing products
    - **Value Objects:** Immutable domain concepts
        - `ProductId` — Typed product identifier (UUID)
        - `QuantityInStockAdjustment` — Stock quantity changes
        - `StockMovement` — Track inventory movement (add, remove, sell)
        - `Image` — Product image metadata and S3 references
    - **Domain Services:** Cross-aggregate business logic
        - `StockService` — Manages inventory adjustments and movements
    - **Domain Events:** Published when product state changes
        - `ProductAddedEvent` — New product created
        - `ProductListedEvent` — Product listed in catalog
        - `ProductDelistedEvent` — Product removed from catalog
        - `ProductPlacedOnSaleEvent` — Product marked on sale
        - `ProductPriceChangedEvent` — Product price updated
        - `ProductRestockedEvent` — Product stock increased
        - `ProductSoldOutEvent` — Product out of stock
    - **Repositories (Ports):** Persistence contracts
        - `ProductRepository` — Product persistence
        - `StockMovementRepository` — Stock movement history
    - **Exceptions:** Domain-specific exceptions
        - `ProductNotFoundException` — Product not found in catalog
        - `DomainException` — Base exception class

2. **Application Layer** (`application/`)
    - **Application Services:** Use case orchestration
        - `ProductApplicationService` — Create, update, list, retrieve products
        - `CategoryApplicationService` — Manage product categories
        - `ProductStorageApplicationService` — Handle image uploads
        - `UploadApplicationService` — Generate pre-signed upload URLs
    - **Event Handlers:** Respond to domain events
    - **DTOs & Mappers:** Data transfer and transformation
    - **Message Publishing:** Handle event publication to external systems

3. **Infrastructure Layer** (`infrastructure/`)
    - **Persistence**: MongoDB repositories and configuration
        - `MongoConfig` — MongoDB connection and replica set setup
        - Product and category persistence adapters
        - `SpringDataAuditingConfig` — Automatic audit field tracking
    - **Storage**: S3/LocalStack integration for image storage
        - `S3ProductImageStorage` — Upload/retrieve images from S3
        - Pre-signed URL generation
    - **Caching**: Redis-based product caching
        - Query result caching for frequently accessed products
    - **Security**: OAuth2 token validation
        - Current user extraction from security context
    - **Message Publishing**: Domain event publishing
    - **Utilities**: Locale configuration, message resolution

4. **Presentation Layer** (`presentation/`)
    - **REST Controllers:** HTTP endpoints
        - `ProductController` — Product CRUD and queries
        - `CategoryController` — Category management
        - `ProductImagesController` — Image management endpoints
        - `UploadRequestController` — Pre-signed upload URL generation
    - **Exception Handling**: Centralized error response formatting
    - **DTOs**: Request/response models

### Security Model

- **OAuth2 Resource Server:** Validates incoming JWT tokens from authorization-server
- **Scope-based access:** Endpoints protected by OAuth2 scopes (e.g., `SCOPE_products:read`, `SCOPE_products:write`)
- **Spring Security method-level annotations** for fine-grained access control

### Event Sourcing & Domain Events

- Domain events are published when products transition state (listed, delisted, placed on sale, etc.)
- Application services listen to domain events and trigger side effects
- Events are used for asynchronous operations (notifications, analytics, synchronization with other services)
- Example: When `ProductListedEvent` fires, caches may be invalidated or notifications sent

### Spring Profiles

The application uses layered profiles:
- `base` — Common configuration
- `development-env` — Local development overrides
- `docker-env` — Docker Compose overrides (MongoDB URLs, Redis configuration, S3 endpoints, etc.)
- `production-env` — Production settings

Activate via `SPRING_PROFILES_ACTIVE=docker` or in application.yml.

## Build Commands

```bash
cd microservices/product-catalog

# Compile and run all tests (unit + integration)
./gradlew build

# Compile only
./gradlew classes

# Run unit tests only
./gradlew test

# Run integration tests (marked with *IT.java)
./gradlew integrationTest

# Run Spring Cloud Contract tests
./gradlew contractTest

# Run all test types (unit + integration + contract)
./gradlew check

# Build runnable JAR
./gradlew bootJar

# Build multi-platform Docker image (linux/arm64, linux/amd64)
./gradlew dockerBuild

# Run a single test class
./gradlew test --tests "com.algaworks.algashop.product.catalog.presentation.ProductControllerTest"

# Run with specific Spring profile
./gradlew build -Pprofile=docker-env
```

## Running Locally

**Start infrastructure (MongoDB, Redis, LocalStack, etc.):**
```bash
cd ../..  # Go to monorepo root
docker compose -f docker-compose.tools.yml up -d
```

**Run the application:**
```bash
# From product-catalog directory
./gradlew bootRun

# With specific profile
SPRING_PROFILES_ACTIVE=docker ./gradlew bootRun
```

The server starts on **port 8083** (configured in application.yml).

**Required /etc/hosts entries** (if not already set):
```
127.0.0.1 algashop-mongodb-1 algashop-mongodb-2 algashop-mongodb-3
127.0.0.1 algashop-localstack s3.algashop-localstack algashop-product-image.algashop-localstack
127.0.0.1 authorization-server
```

## Project Structure

```
src/main/java/com/algaworks/algashop/product/catalog/
├── domain/
│   └── model/
│       ├── IdGenerator.java                    # ID generation utility
│       ├── DomainException.java                # Base domain exception
│       ├── DomainEntityNotFoundException.java   # Not found exception
│       ├── DomainEventPublisher.java          # Event publishing
│       ├── category/
│       │   ├── ProductCategory.java           # Category aggregate
│       │   └── [related classes]
│       └── product/
│           ├── Product.java                   # Product aggregate root
│           ├── ProductId.java                 # Typed product ID
│           ├── Image.java                     # Product image metadata
│           ├── ProductRepository.java         # Repository port
│           ├── QuantityInStockAdjustment.java # Stock adjustment value object
│           ├── StockMovement.java             # Stock movement tracking
│           ├── StockMovementRepository.java   # Stock movement persistence port
│           ├── StockService.java              # Stock management domain service
│           ├── ProductNameProjection.java     # Product projection for queries
│           ├── ProductAddedEvent.java         # Domain events
│           ├── ProductListedEvent.java
│           ├── ProductDelistedEvent.java
│           ├── ProductPlacedOnSaleEvent.java
│           ├── ProductPriceChangedEvent.java
│           ├── ProductRestockedEvent.java
│           ├── ProductSoldOutEvent.java
│           ├── ProductNotFoundException.java  # Product-specific exception
│           └── [related classes]
├── application/
│   ├── ApplicationMessagePublisher.java       # Event publishing
│   ├── PageModel.java                         # Pagination model
│   ├── ResourceNotFoundException.java         # Application-level exception
│   ├── category/
│   │   ├── CategoryApplicationService.java
│   │   ├── CategoryQueryService.java
│   │   ├── [DTOs, handlers]
│   │   └── [related classes]
│   ├── product/
│   │   ├── ProductApplicationService.java     # Product creation/update
│   │   ├── ProductQueryService.java           # Product queries
│   │   ├── [DTOs, handlers]
│   │   └── [related classes]
│   ├── storage/
│   │   ├── ProductStorageApplicationService.java
│   │   ├── [related classes]
│   │   └── [DTOs]
│   ├── upload/
│   │   ├── UploadApplicationService.java      # Pre-signed URL generation
│   │   ├── [related classes]
│   │   └── [DTOs]
│   ├── security/
│   │   └── CurrentUserProvider.java           # Extract current user from security context
│   └── utility/
│       └── [utility classes]
├── infrastructure/
│   ├── async/
│   │   └── [async processing]
│   ├── cache/
│   │   ├── CacheConfiguration.java            # Redis cache setup
│   │   └── [cache adapters]
│   ├── listener/
│   │   └── [domain event listeners]
│   ├── locale/
│   │   └── [localization support]
│   ├── message/
│   │   └── [message resolution]
│   ├── persistence/
│   │   ├── MongoConfig.java                   # MongoDB configuration
│   │   ├── SpringDataAuditingConfig.java      # Audit field setup
│   │   ├── category/
│   │   │   └── [category persistence adapters]
│   │   ├── dataload/
│   │   │   └── [data loading utilities]
│   │   └── product/
│   │       ├── ProductMongoRepository.java    # Spring Data MongoDB repository
│   │       ├── ProductRepositoryAdapter.java  # Adapter implementing repository port
│   │       ├── StockMovementMongoRepository.java
│   │       ├── StockMovementRepositoryAdapter.java
│   │       └── [related classes]
│   ├── security/
│   │   └── [OAuth2 configuration]
│   ├── storage/
│   │   ├── S3ProductImageStorage.java         # S3 integration
│   │   ├── PresignedUrlManager.java           # Pre-signed URL generation
│   │   └── [AWS-related classes]
│   └── utility/
│       └── [utility functions]
├── presentation/
│   ├── ApiExceptionHandler.java               # Global exception handler
│   ├── UnprocessableContentException.java     # Validation exception
│   ├── ProductQuantityModel.java              # Request model for quantities
│   ├── ProductController.java                 # Product REST endpoints
│   │                                          # GET /api/v1/products
│   │                                          # POST /api/v1/products
│   │                                          # PUT /api/v1/products/{productId}
│   │                                          # GET /api/v1/products/{productId}
│   ├── CategoryController.java                # Category REST endpoints
│   │                                          # GET /api/v1/categories
│   │                                          # POST /api/v1/categories
│   ├── ProductImagesController.java           # Image management endpoints
│   │                                          # POST /api/v1/products/{productId}/images
│   │                                          # DELETE /api/v1/products/{productId}/images/{fileName}
│   ├── UploadRequestController.java           # Pre-signed URL endpoints
│   │                                          # POST /api/v1/upload-requests
│   └── [related classes]
└── ProductCatalogApplication.java
```

## Database (MongoDB)

MongoDB collections are created on startup:

**Collections:**
- `product` — Product documents with name, description, price, category, stock
- `product-category` — Product categories
- `stock-movements` — Audit trail of inventory movements

**Key Features:**
- Replica set support for transactions (docker-compose.yml configures 3-node replica set)
- Automatic createdAt/updatedAt fields via Spring Data Auditing
- TTL indexes for temporary collections if needed

**To access MongoDB locally:**
```bash
# Connect via Mongo shell (after starting docker-compose.tools.yml)
mongosh --host localhost:27017 --username admin --password admin
use algashop
db.product.find()
```

## Testing

Test structure mirrors source structure:

```
src/test/java/com/algaworks/algashop/product/catalog/
├── domain/
│   └── model/
│       ├── product/
│       │   └── ProductTest.java
│       └── ...
├── application/
│   ├── product/
│   │   └── ProductApplicationServiceTest.java
│   └── ...
└── presentation/
    └── ProductControllerTest.java
```

### Test Types

- **Unit tests** (`*Test.java`): Test application services, domain logic, value objects
    - Use `@SpringBootTest` for tests needing Spring context
    - Use plain JUnit for pure domain logic
    - Mock external dependencies (HTTP clients, repositories, storage)

- **Integration tests** (`*IT.java`): Use TestContainers for embedded MongoDB
    - Test persistence layer with real database
    - Test application services with real repositories
    - Use `@DataMongoTest` for repository-focused tests

- **Contract tests** (`*ContractTest.java`): Spring Cloud Contract verifier tests
    - Define contracts for API endpoints
    - Auto-generate consumer stubs

Example:
```java
@SpringBootTest
class ProductApplicationServiceTest {
    @Test
    void shouldCreateProductWithValidData() { ... }
    
    @Test
    void shouldThrowExceptionWhenProductNotFound() { ... }
}
```

## Key Concepts

### Domain-Driven Design Patterns

**Aggregates:** Self-contained units with invariants
- `Product` — Owns product details, pricing, stock information
- `ProductCategory` — Category metadata and relationships

**Value Objects:** Immutable, no identity
- `ProductId` — Typed ID instead of raw UUID
- `QuantityInStockAdjustment` — Stock adjustment with validation
- `Image` — Product image metadata

**Domain Events:** Important state changes
- Published when product state changes (listed, delisted, price changed, restocked, etc.)
- Application services subscribe and handle side effects
- Example: When `ProductRestockedEvent` fires, cache is invalidated

**Domain Services:** Cross-aggregate logic
- `StockService` — Manages inventory adjustments and tracks movements

### Product Lifecycle

Products transition through states:
```
CREATED → LISTED → ON_SALE / DELISTED
```

Status transitions enforce business rules (e.g., cannot go from ON_SALE back to CREATED).

### Stock Management

- Stock tracked through `StockMovement` records (audit trail)
- Adjustments recorded for add, remove, sell operations
- Separate queries for stock availability
- Events published when stock levels change (restocked, sold out)

### Image Storage

- Product images stored in S3/LocalStack
- Pre-signed URLs generated for secure access
- Async image processing if needed
- Metadata tracked in MongoDB

### Caching Strategy

- Redis caches frequently accessed products
- Cache invalidation on product updates
- Configurable TTL per cache entry

## Dependencies & External Integrations

### Internal Services

- **Ordering Service** (port 8080)
    - Queries products for shopping cart and order creation
    - Calls `GET /api/v1/products/{productId}` via ProductCatalogAPIClient

### External Services (via HTTP clients)

None directly, but integrates with:
- **Authorization Server** (port 8081) — OAuth2 token validation
- **AWS S3** (LocalStack in development) — Image storage

### Libraries

- **MongoDB Spring Data** — Reactive/non-reactive data access
- **Spring Security OAuth2** — Token validation
- **AWS SDK** — S3 integration
- **Spring Cloud Contract** — Contract-driven testing
- **TestContainers** — Embedded MongoDB for testing
- **Lombok** — Boilerplate reduction
- **ModelMapper** — DTO/Entity mapping

## Recent Changes

- **Domain events**: Published on product state changes
- **Stock tracking**: Movement history and audit trail
- **Image management**: S3/LocalStack integration with pre-signed URLs
- **Caching layer**: Redis caching for product queries
- **Pagination**: Page-based product listing with filters

## Common Tasks

### Adding a New Product Property

1. Add field to `Product` aggregate in `domain/model/product/`
2. Update MongoDB document mapping (if using custom serialization)
3. Create application service method to update the property
4. Add REST endpoint in `ProductController`
5. Add validation using Bean Validation annotations
6. Write unit test for service method
7. Write integration test with MongoDB

### Adding a New Product State/Event

1. Create new domain event class (e.g., `ProductDiscountedEvent`)
2. Add event publishing logic to `Product` aggregate
3. Create event handler in application layer if side effects needed
4. Write tests for event publishing and handling

### Integrating with External Storage Service

1. Create storage adapter interface (e.g., `ProductImageStorage`)
2. Implement S3 adapter in `infrastructure/storage/`
3. Inject into application service
4. Add mock implementation for testing
5. Write integration test with LocalStack

### Adding a New Query/Filter

1. Add query method to `ProductRepository` port
2. Implement in MongoDB adapter
3. Create application service method combining multiple repositories if needed
4. Add REST endpoint with pagination
5. Write integration test querying the database

### Publishing a Domain Event

1. Create event class (e.g., `ProductLaunchDateSetEvent extends DomainEvent`)
2. Publish from aggregate: `DomainEventPublisher.publishEvent(...)`
3. Create event handler in application layer
4. Subscribe handler via Spring `@EventListener` or `@TransactionalEventListener`
5. Test event is published and handler executes

## Notes for Future Work

- Product search using Elasticsearch or MongoDB text search
- Product recommendations based on purchase history
- Product reviews and ratings
- Bulk product import/export
- Advanced inventory forecasting
- Product variants (size, color, etc.)
- Real-time product availability sync with 3rd-party retailers
- Analytics dashboard for product performance
- Internationalization for product descriptions and categories
- Product digital assets/documentation


