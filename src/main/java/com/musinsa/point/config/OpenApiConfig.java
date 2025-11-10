package com.musinsa.point.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 설정
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("무신사 무료 포인트 시스템 API")
                .description("무료 포인트 적립, 사용, 취소 기능을 제공하는 RESTful API")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Musinsa Payments")
                    .email("payments@musinsa.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server")
            ));
    }
}
