package com.senac.tsi.MinhaPrimeiraApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI(@Value("$(springdoc.version)") String appVersion){
        return new OpenAPI()
                .info(new Info()
                        .title("API of Employees")
                        .version(appVersion)
                        .description("API to manage employees")
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("TSI")
                                .url("https://www.senac.com.br")
                                .email("****@sp.senac.br"))
                );
    }
}
