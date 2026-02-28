// config/OpenApiConfig.java
package com.agilesolutions.account.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Account Update Service API",
                description = "REST API refactored from COBOL COACTUPC.cbl - Account Management",
                version = "1.0.0",
                contact = @Contact(
                        name = "Agile Solutions",
                        email = "support@agilesolutions.com"
                )
        ),
        servers = {
                @Server(url = "/api/v1", description = "Default server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {}