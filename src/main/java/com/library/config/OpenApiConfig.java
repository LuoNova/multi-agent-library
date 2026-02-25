package com.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Swagger/OpenAPI配置
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("多智能体图书馆系统API")
                        .description("基于Contract Net协议的跨馆资源调度接口")
                        .version("v1.0")
                        .contact(new Contact().name("开发者").email("dev@example.com")));
    }
}