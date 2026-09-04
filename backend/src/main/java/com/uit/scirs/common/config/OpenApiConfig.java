package com.uit.scirs.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Live API documentation (Swagger UI at /swagger-ui.html, raw spec at
 * /v3/api-docs) generated from the controllers/DTOs themselves — see
 * api-standards.md for the human-authored version of the same contract.
 * Registers the JWT Bearer scheme so requests can be authorized in the UI:
 * log in via POST /api/auth/login, then click "Authorize" and paste the
 * returned token.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI scirsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SCIRS API")
                        .description("Smart Community Issue Report System — REST API. "
                                + "See context-kit/api-standards.md for the full documented contract "
                                + "(status codes, role-based access matrix, business rules).")
                        .version("v1")
                        .contact(new Contact().name("CST-4105 Section-C, Group-II")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
