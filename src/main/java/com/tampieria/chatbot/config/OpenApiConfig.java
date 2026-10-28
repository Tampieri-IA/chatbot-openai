package com.tampieria.chatbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatbotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chatbot OpenAI API - Tampieri IA")
                        .description("API de chatbot integrada com a OpenAI, construída com Spring Boot + RestClient")
                        .version("v1.0"));
    }
}
