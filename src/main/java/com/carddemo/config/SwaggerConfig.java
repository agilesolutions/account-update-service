package com.carddemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cardUpdateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Update Service API")
                        .description("Credit Card Update Microservice - Migrated from COBOL COCRDUPC.cbl\n\n" +
                                "This service handles credit card data update operations, " +
                                "replacing the original CICS/VSAM based COBOL implementation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mainframe Modernization Team")
                                .email("modernization@carddemo.com"))
                        .license(new License()
                                .name("Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.carddemo.com").description("Production")
                ));
    }
}