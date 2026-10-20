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
                registry.addMapping("/**") // Libera todos os endpoints da API
                        .allowedOriginPatterns(
                                //"http://localhost:[*]",   // Libera qualquer porta no localhost
                                //"http://127.0.0.1:[*]"    // Libera qualquer porta no 127.0.0.1
                                "*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*") // Libera todos os cabeçalhos das requisições
                        .allowCredentials(true); // Permite envio de cookies/autenticação se necessário
            }
        };
    }
}
