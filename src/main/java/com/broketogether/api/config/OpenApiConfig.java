package com.broketogether.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
        .components(new Components().addSecuritySchemes("Bearer Auth",
            new SecurityScheme().name("Bearer Auth").type(SecurityScheme.Type.HTTP).scheme("bearer")
                .bearerFormat("JWT")));
  }
}
