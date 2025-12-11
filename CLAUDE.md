# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Macula Boot is a Spring Boot-based microservices development framework built on Spring Cloud Alibaba and Spring Cloud Tencent. It provides a comprehensive set of starter modules and tools for building microservices applications.

**Key Technologies:**
- Spring Boot 3.5.8
- Spring Cloud 2023.0.3
- Java 17
- Maven-based build system

## Building and Testing

### Common Maven Commands

```bash
# Build all modules without running tests (fast build)
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy

# Run tests
mvn test

# Run a specific test
mvn test -Dtest=ClassName#methodName

# Build specific module
cd macula-boot-starters/macula-boot-starter-redis
mvn clean install

# Run integration tests
mvn verify

# Run examples
cd macula-boot-examples/macula-example-gateway
mvn spring-boot:run
```

### Build Profiles

- **default**: Standard build
- **deploy**: Used for publishing to Maven Central (required for releases)

### Release Process

```bash
# Automated release via script (updates version, creates tag, deploys)
./release.sh 6.0.0

# Manual release (GitHub Actions also handles this)
mvn clean deploy --batch-mode -Pdeploy -DskipTests=true
```

CI/CD:
- GitHub Actions automatically deploys snapshots on push to main
- Releases are published to Maven Central via GitHub Actions

## High-Level Architecture

### Module Structure

```
macula-boot/
├── macula-boot-parent/              # Parent POM - manages all dependencies and versions
├── macula-boot-commons/             # Common utilities (response wrappers, helpers, exceptions)
├── macula-boot-starters/            # 30+ starter modules for various services
│   ├── macula-boot-starter-web/            # Web support
│   ├── macula-boot-starter-redis/          # Redis (Redisson)
│   ├── macula-boot-starter-mybatis-plus/   # MyBatis Plus
│   ├── macula-boot-starter-rocketmq/       # RocketMQ
│   ├── macula-boot-starter-kafka/          # Kafka
│   ├── macula-boot-starter-security/       # OAuth2 security
│   ├── macula-boot-starter-sentinel/       # Sentinel (circuit breaker)
│   ├── macula-boot-starter-seata/          # Distributed transaction
│   ├── macula-boot-starter-cloud/          # Cloud integrations
│   │   ├── macula-boot-starter-cloud-gateway/        # API Gateway
│   │   ├── macula-boot-starter-cloud-alibaba/        # Spring Cloud Alibaba
│   │   ├── macula-boot-starter-cloud-tencent/        # Spring Cloud Tencent
│   │   └── macula-boot-starter-cloud-tsf/            # TSF integration
│   └── [25+ other starters...]
├── macula-boot-examples/            # Sample applications
│   ├── macula-example-gateway/      # Gateway example
│   ├── macula-example-consumer/     # Service consumer
│   ├── macula-example-provider/     # Service provider
│   ├── macula-example-ddd/          # DDD architecture example
│   └── [8+ other examples...]
└── macula-boot-archetype/           # Maven archetypes
```

### Cloud Architecture

The framework supports multiple cloud service providers:

1. **Spring Cloud Alibaba** (Nacos, Sentinel, Seata)
2. **Spring Cloud Tencent** (PolarisMesh)
3. **Tencent TSF** (Tencent Service Framework)

### Configuration Patterns

- **bootstrap.yml**: Cloud configuration (service discovery, sentinel, etc.)
- **application.yml**: Application-specific settings
- Examples use `profile.active` (local/dev/stg/pet/prd)

Example configuration (gateway):
```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: true
        namespace: ${nacos.namespace}
    sentinel:
      eager: true
      transport:
        dashboard: localhost:8080
        port: 8719
    gateway:
      routes: [...]
```

## Key Components

### Service Discovery & Configuration
- **Nacos**: Service discovery and configuration management
- Bootstrap configuration required for cloud services

### Service Governance
- **Sentinel**: Circuit breaker, flow control,熔断降级
- **Seata**: Distributed transaction management

### API Gateway
- Spring Cloud Gateway with OAuth2 integration
- Custom security configuration
- CORS support
- Route configuration in application.yml

### Data Access
- MyBatis Plus integration
- JPA support
- Druid connection pool

### Messaging
- RocketMQ
- Kafka
- Sender (database-backed async messaging)

### Other Services
- Redis (Redisson)
- Distributed locks (lock4j)
- ID generation (TinyID)
- Task scheduling (XXL-Job, Snail-Job)
- Object storage (MinIO, Alibaba OSS, Tencent COS, AWS S3)
- WeChat SDK integration

## Development Workflows

### Adding a New Starter Module

1. Create module under `macula-boot-starters/`
2. Add to parent POM dependencyManagement
3. Create Spring Boot starter configuration in `src/main/resources/META-INF/spring/`
4. Add autoconfiguration class with `@Configuration` and `@ConditionalOnClass`
5. Add properties class with `@ConfigurationProperties`
6. Create README.md with usage examples
7. Add example in `macula-boot-examples/`

### Starter Module Structure

```
macula-boot-starter-xxx/
├── src/
│   ├── main/
│   │   ├── java/dev/macula/boot/starter/xxx/
│   │   │   ├── config/          # Auto-configuration
│   │   │   ├── properties/      # Configuration properties
│   │   │   └── XXXAutoConfiguration.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── spring/
│   │               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── test/
│       └── java/...
└── pom.xml
```

### Testing Guidelines

- Place tests in `src/test/java/`
- Use Spring Boot Test annotations (`@SpringBootTest`, `@Test`)
- Examples have `Application.java` test classes
- Mock external dependencies (Redis, Kafka, Nacos)

### Common Patterns

1. **Configuration Properties**: Use `@ConfigurationProperties` with prefix
2. **Conditional Loading**: Use `@ConditionalOnClass`, `@ConditionalOnProperty`
3. **Auto-configuration**: Mark with `@AutoConfigurationPackage` and `@Import`
4. **Starter Registration**: Add to `spring.factories` or `AutoConfiguration.imports`

## Dependency Management

All dependencies are managed in `macula-boot-parent/pom.xml`:

- **Spring Boot**: 3.5.8
- **Spring Cloud**: 2023.0.3
- **Spring Cloud Alibaba**: 2023.0.3.4
- **Spring Cloud Tencent**: 2.1.0.0-2023.0.6
- **MySQL Connector**: 9.5.0
- **Redisson**: 3.52.0
- **MapStruct**: 1.6.3
- **XXL-Job**: 3.1.1
- **Dubbo**: 3.3.6

## Important Files

- `pom.xml`: Root POM with modules definition
- `macula-boot-parent/pom.xml`: Parent POM with dependency management
- `release.sh`: Release script for versioning and tagging
- `.github/workflows/`: CI/CD workflows
- `README.md`: Project documentation
- `Roadmap.md`: Future development plans

## Version Information

- **Current Version**: 6.0.0-SNAPSHOT
- **Java Requirement**: 17+
- **Maven**: 3.6+

## Documentation

Each starter module has its own README.md with:
- Features overview
- Usage examples
- Configuration properties
- Dependencies required

## Support

- Project Website: https://macula.dev
- GitHub: https://github.com/macula-projects/macula-boot
