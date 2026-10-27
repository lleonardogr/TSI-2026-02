package com.senac.tsi.MinhaPrimeiraApi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Applies CORS to all endpoints
                        .allowedOrigins("*") // Public API: any site can call it
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Allowed HTTP methods
                        .allowedHeaders("*") // Allowed request headers (incl. Authorization)
                        .exposedHeaders("Location", "Idempotent-Replayed") // Response headers browsers may read
                        .maxAge(3600); // Cache preflight response for 1 hour
            }
        };
    }
}