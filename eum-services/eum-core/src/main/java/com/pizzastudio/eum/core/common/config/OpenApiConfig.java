package com.pizzastudio.eum.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * API 문서 설정.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        Components components = new Components()
            .addSecuritySchemes("Authorization", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));

        return new OpenAPI()
            .components(components)
            .addSecurityItem(new SecurityRequirement().addList("Authorization"))
            .info(new Info()
                .title("이음 API")
                .description("소상공인 지원금 신청·심사 시스템")
                .version("1.0.0"));
    }
}
