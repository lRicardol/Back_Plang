package com.tourism.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Local-profile convenience: GET / redirects to Swagger UI so the API can be
 * tested straight from the browser at {@code http://localhost:8080/}.
 * Only active under the {@code local} profile (app served at the root path).
 */
@Configuration
@Profile("local")
public class SwaggerUiRedirectConfig {

    @Bean
    public WebMvcConfigurer swaggerUiRedirectConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addRedirectViewController("/", "/swagger-ui/index.html");
            }
        };
    }
}
