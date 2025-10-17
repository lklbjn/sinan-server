# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Running the Application
```bash
# Development mode (using Maven)
mvn spring-boot:run

# Or run the main class directly from IDE
pres.peixinyi.sinan.SinanApplicationRun
```

### Building
```bash
# Build JAR package (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# JAR will be output to ./jar/sinan.jar
```

### Testing
```bash
# Run all tests
mvn test

# Note: This project currently has no test files in src/test/ or test/ directories
```

## Architecture Overview

### Module Structure
The application follows a modular architecture with clear separation of concerns:

- **Module `rbac`**: User authentication, credentials, and role-based access control
  - User management with local auth, GitHub OAuth2, and PassKey support
  - API key authentication for external access
  - User credential management and data export/import

- **Module `sinan`**: Core bookmark management functionality
  - Bookmark CRUD operations with space and tag organization
  - Space sharing and bookmark receiving/sharing
  - Ignored groups for bookmark imports

- **Module `favicon`**: Website icon extraction and caching service
  - Automatic favicon extraction from URLs
  - Local caching with fallback mechanisms

- **Module `github`**: GitHub OAuth2 integration
  - Handles GitHub authentication flow
  - User profile retrieval and account linking

### Database Architecture
- **MySQL 8.0+** as primary database with MyBatis Plus ORM
- **Redis** for caching user sessions and application data
- **Flyway** for database migrations (enabled by default)
- Logical deletion implemented (`deleted` field)

### Key Technologies
- **Spring Boot 3.2.4** with Java 17
- **Sa-Token 1.44.0** for authentication and authorization
- **MyBatis Plus 3.5.12** for database operations
- **Spring AI** with OpenAI integration for website analysis
- **WebAuthn** for PassKey support

### Configuration Management
- Environment-specific profiles (`application-dev.yaml`, `application.yaml`)
- Heavy use of environment variables for configuration
- Key configuration areas:
  - Database connection (MySQL)
  - Redis configuration
  - OpenAI API integration
  - Mail server settings
  - File upload paths
  - PassKey/WebAuthn settings

### API Design
- RESTful APIs with `/api` context path
- Dual authentication methods: session-based (Sa-Token) and API key-based
- Standardized response format via `Result.java`
- Comprehensive DTO structure for request/response objects

### Security Features
- Multi-factor authentication support (local + OAuth2 + PassKey)
- API key authentication for external integrations
- Logical deletion for data protection
- Configuration-driven CORS and security settings

## Development Notes

### Database Migrations
- Flyway is enabled and manages database schema changes
- Migration files located in `classpath:db/migration`
- Baseline version set to 1 with `baseline-on-migrate: true`

### File Structure
- Source code organized by functional modules under `src/main/java/pres/peixinyi/sinan/module/`
- DTOs separated into request/response packages
- Entity classes map directly to database tables
- MyBatis XML mappers in `src/main/resources/mapper/`

### External Integrations
- GitHub OAuth2 for third-party login
- OpenAI API for intelligent website categorization
- Email service for user notifications
- PassKey/WebAuthn for passwordless authentication

### Performance Considerations
- Redis caching for user sessions and frequently accessed data
- Logical deletion to preserve data integrity
- Database connection pooling configured
- Flyway migrations for consistent schema management