# COACTUPC → Java Migration

## What this is

A direct Java translation of the Amazon CardDemo COBOL account maintenance program `COACTUPC.CBL`.

## Summary COBOL → Java migration notes
- Claude code direct migration COACTUPC COBOL module to java. [See original COBOL source on github](https://github.com/hpatel-appliedai/aws-mainframe-modernization-carddemo/blob/main/app/cbl/COACTUPC.cbl)
- Replaced maven with gradle
- Added all generated java sources (model, repository, service and other components)
- Added junit test.
- Ported indexed sequential files to PostgreSQL storage and JPA
- Enabled Webflux and swagger API endpoints
- Enable lombok, remove all generated getters and setters
- Enable postgresql test container tests
- Enable spring security with JWT authentication and role-based access control
- Added global exception handling with custom exceptions and error responses
- Added audit logging of account updates to a separate database table
- Added comprehensive unit and integration tests covering all business logic and edge cases
- Enabled environment-specific configuration with Spring profiles and externalized properties to Spring Config Server
- Enabled service discovery and load balancing with Spring Cloud Netflix Eureka and Ribbon
- Enabled routing and API gateway with Spring Cloud Gateway
- Added input validation with Hibernate Validator and custom validation annotations
- Added detailed API documentation with OpenAPI/Swagger annotations
- Enabled Spring Boot Actuator for monitoring and health checks
- Added Dockerfile for containerization and deployment
- Added CI/CD pipeline configuration for automated testing and deployment (e.g. GitHub Actions, Jenkins, etc.)
- Added Kustomize configuration for Kubernetes deployment


[Please read my architectural design decisions I put up with the support of Caude Code](docus/prompt.md) 


**before diving into the code. It will give you important context on how I approached the migration and why I made certain design choices!!!...**

## Code structure
```  
account-update-service/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/agilesolutions/account/
│   │   │   ├── AccountUpdateServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AccountController.java
│   │   │   │   └── AuthController.java
│   │   │   ├── domain/
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Account.java
│   │   │   │   │   ├── AccountType.java
│   │   │   │   │   └── AuditLog.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── AccountRequestDto.java
│   │   │   │   │   ├── AccountResponseDto.java
│   │   │   │   │   ├── AccountUpdateDto.java
│   │   │   │   │   └── AuthDto.java
│   │   │   │   └── enums/
│   │   │   │       └── AccountStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── AccountNotFoundException.java
│   │   │   │   ├── BusinessValidationException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── mapper/
│   │   │   │   └── AccountMapper.java
│   │   │   ├── repository/
│   │   │   │   ├── AccountRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/
│   │   │   │   ├── AccountService.java
│   │   │   │   ├── AccountServiceImpl.java
│   │   │   │   ├── AuditService.java
│   │   │   │   └── ValidationService.java
│   │   │   └── util/
│   │   │       ├── AccountConstants.java
│   │   │       └── DateUtils.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_accounts_table.sql
│   │           ├── V2__create_audit_log_table.sql
│   │           └── V3__create_users_table.sql
│   └── test/
│       └── java/com/agilesolutions/account/
│           ├── controller/
│           │   └── AccountControllerTest.java
│           ├── service/
│           │   └── AccountServiceTest.java
│           └── integration/
│               └── AccountIntegrationTest.java
```





