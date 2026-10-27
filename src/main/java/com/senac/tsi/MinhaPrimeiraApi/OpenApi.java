package com.senac.tsi.MinhaPrimeiraApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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

    // One Swagger document per API version (pick it in the "Select a definition" dropdown of the Swagger UI).
    // A single OpenAPI document can't hold v1 and v2 because both use the same path + HTTP method.
    @Bean
    public GroupedOpenApi v1Api() {
        return versionGroup("1");
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return versionGroup("2");
    }

    private static GroupedOpenApi versionGroup(String version) {
        return GroupedOpenApi.builder()
                .group("v" + version)
                .displayName("Employees API v" + version)
                // Keep the endpoints of this version plus the unversioned ones (they serve every version)
                .addOpenApiMethodFilter(method -> {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    return mapping == null || mapping.version().isEmpty() || mapping.version().equals(version);
                })
                // Pre-fill the API-Version header with this document's version
                .addOperationCustomizer((operation, handlerMethod) -> {
                    if (operation.getParameters() != null) {
                        operation.getParameters().stream()
                                .filter(parameter -> ApiVersionConfig.HEADER.equals(parameter.getName()))
                                .forEach(parameter -> parameter.schema(new StringSchema()._default(version)._enum(List.of(version))));
                    }
                    return operation;
                })
                .build();
    }
}
