You will be acting as a software architect and migration specialist tasked with migrating legacy COBOL monolithic applications to modern cloud-native Java Spring Boot microservices.

Here is the COBOL code that needs to be migrated:
- [This is the COBOL code under translation](https://github.com/hpatel-appliedai/aws-mainframe-modernization-carddemo/blob/main/app/cbl/COCRDUPC.cbl)
- [Location of COBOL copy books](https://github.com/hpatel-appliedai/aws-mainframe-modernization-carddemo/tree/main/app/cpy)

Here are the specific requirements and constraints for this migration:

<requirements>
{{REQUIREMENTS}}
</requirements>

Your task is to analyze the COBOL code and provide a comprehensive migration plan to convert it into cloud-native Java Spring Boot microservices that meets all specified requirements.

Before providing your final migration plan, you must first conduct a thorough analysis. Write your analysis inside <analysis> tags and think through the following aspects:

1. **COBOL Code Analysis**:
    - Identify all main business functions and their purposes
    - Map out data structures including COBOL records, files, copybooks, and working storage sections
    - Trace the program flow and control logic (PERFORM statements, GO TO, conditional logic)
    - Identify all dependencies including called programs, external files, and database interactions
    - Note any COBOL-specific constructs that will require special handling (REDEFINES, OCCURS, etc.)

2. **Microservice Decomposition Strategy**:
    - Determine logical boundaries for breaking down the monolith based on business capabilities
    - Identify bounded contexts using Domain-Driven Design principles
    - Determine data ownership for each proposed microservice
    - Consider transaction boundaries and consistency requirements
    - Evaluate coupling and cohesion to ensure proper service boundaries

3. **Data Migration Strategy**:
    - Map COBOL data structures (PIC clauses, COMP fields, etc.) to appropriate Java types and database columns
    - Determine appropriate database technology for each microservice (relational vs. NoSQL)
    - Plan for handling COBOL file systems (VSAM, sequential files, indexed files)
    - Consider data consistency and referential integrity across services

4. **Integration Points**:
    - Identify all external system integrations and how they should be modernized
    - Analyze batch processes and determine if they should become scheduled jobs, event-driven processes, or real-time APIs
    - Map transaction boundaries and determine appropriate distributed transaction patterns (Saga, 2PC alternatives)
    - Identify synchronous vs. asynchronous communication needs

5. **Cloud-Native Considerations**:
    - Plan for service discovery and registration mechanisms
    - Design configuration management approach for different environments
    - Incorporate resilience patterns (circuit breakers, bulkheads, retries, timeouts)
    - Plan observability strategy (logging, metrics, distributed tracing)
    - Consider scalability and elasticity requirements
    - Address security concerns (authentication, authorization, secrets management)

After completing your analysis, provide your comprehensive migration plan inside <migration_plan> tags with the following sections:

**1. Executive Summary**
Provide a concise high-level overview (3-5 paragraphs) covering:
- The overall migration approach and strategy
- Key architectural decisions and their rationale
- Major benefits of the proposed architecture
- High-level timeline and effort estimation

**2. Microservice Architecture Design**
For each proposed microservice, specify:
- Service name and primary responsibilities
- Bounded context definition and business capability alignment
- Data ownership (which entities/tables this service owns)
- APIs exposed (REST endpoints, events published)
- Dependencies on other services
- Communication patterns (synchronous REST, asynchronous messaging, event streaming)

Also include:
- An overall architecture diagram description showing service interactions
- API Gateway strategy
- Service mesh considerations (if applicable)

**3. Technology Stack**
Provide specific recommendations for:
- Java version (e.g., Java 17 LTS) with justification
- Spring Boot version and key Spring modules (Spring Data, Spring Security, Spring Cloud, etc.)
- Spring Cloud components: Config Server, Eureka/Consul, Gateway, Circuit Breaker, etc.
- Database technologies for each microservice with rationale
- Message broker or event streaming platform (Kafka, RabbitMQ, AWS SQS/SNS, etc.)
- Caching strategy (Redis, Hazelcast, etc.)
- Cloud platform recommendations (AWS, Azure, GCP) with specific services
- Container orchestration (Kubernetes, ECS, etc.)
- CI/CD tooling recommendations

**4. Data Migration Strategy**
Provide detailed guidance on:
- Mapping table: COBOL data structures → Java entities/DTOs → Database schemas
- Specific handling of COBOL constructs (COMP-3 to BigDecimal, PIC X to String, etc.)
- Database schema design for each microservice (include key tables and relationships)
- Data migration approach: big bang vs. strangler pattern vs. incremental
- Strategy for COBOL file systems (VSAM → relational DB, sequential files → object storage, etc.)
- Data synchronization strategy during transition period
- Handling of legacy data formats and encodings (EBCDIC to ASCII, etc.)

**5. Implementation Guidance**
For each identified microservice, provide:
- Recommended package structure (e.g., com.company.service.controller, service, repository, model, config)
- Core Java classes needed (entities, DTOs, mappers)
- Key Spring Boot annotations and their purposes (@RestController, @Service, @Repository, @Transactional, etc.)
- REST API endpoint designs with HTTP methods, paths, request/response formats
- Repository/DAO layer approach (Spring Data JPA, MyBatis, JDBC Template)
- Service layer structure and business logic transformation approach
- Exception handling strategy
- Validation approach (Bean Validation, custom validators)

**6. Cloud-Native Patterns Implementation**
Specify how to implement:
- **Service Discovery**: Technology choice and configuration approach
- **Configuration Management**: Externalized configuration strategy (Spring Cloud Config, Kubernetes ConfigMaps, etc.)
- **Resilience Patterns**:
    - Circuit breaker implementation (Resilience4j, Spring Cloud Circuit Breaker)
    - Retry logic with exponential backoff
    - Timeout configurations
    - Bulkhead pattern for resource isolation
- **API Gateway**: Routing, authentication, rate limiting, request transformation
- **Logging Strategy**: Structured logging, correlation IDs, log aggregation
- **Monitoring and Observability**: Metrics (Micrometer, Prometheus), health checks, distributed tracing (Zipkin, Jaeger)
- **Security**: Authentication (OAuth2, JWT), authorization (Spring Security), secrets management
- **Data Consistency**: Saga pattern or eventual consistency approaches for distributed transactions

**7. Migration Roadmap**
Provide a phased approach including:
- **Phase breakdown**: What gets migrated in each phase and why
- **Strangler pattern application**: How to incrementally replace COBOL functionality
- **Risk mitigation strategies**: For each major risk, provide mitigation approach
- **Testing strategy**:
    - Unit testing approach (JUnit 5, Mockito)
    - Integration testing (TestContainers, Spring Boot Test)
    - Contract testing (Pact, Spring Cloud Contract)
    - End-to-end testing strategy
    - Performance testing approach
- **Rollback plans**: How to safely rollback if issues arise
- **Training and knowledge transfer**: Plan for team upskilling
- **Timeline estimates**: Realistic timeframes for each phase

**8. Code Examples**
Provide concrete, production-ready Java Spring Boot code snippets demonstrating:

a) **Entity Classes**: Show transformation of COBOL records to JPA entities with proper annotations

b) **Repository Interfaces**: Spring Data JPA or custom repository implementations

c) **Service Layer**: Business logic transformation from COBOL PROCEDURE DIVISION to Java service methods

d) **REST Controllers**: API endpoints with proper request/response handling, validation, and error handling

e) **Configuration Classes**: Spring Boot configuration for databases, messaging, security, etc.

f) **DTO Classes**: Data transfer objects for API contracts

g) **Exception Handling**: Global exception handler with @ControllerAdvice

h) **Integration Examples**: If applicable, show message producer/consumer or event handling code

For each code example:
- Include necessary imports
- Add comments explaining the transformation from COBOL concepts
- Use proper Spring Boot and Java best practices
- Show realistic business logic, not just boilerplate

Important guidelines for your migration plan:
- Ensure all requirements specified in the requirements section are explicitly addressed
- Focus on practical, implementable solutions rather than theoretical concepts
- Prioritize maintainability, scalability, and operational excellence
- Consider the team's ability to support the new architecture
- Address both functional and non-functional requirements
- Be specific with technology choices and provide rationale
- Consider cost implications of architectural decisions

Your final output should contain only the <analysis> section followed by the <migration_plan> section. Do not include any preamble or additional commentary outside these sections.