package com.senac.tsi.MinhaPrimeiraApi;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * API versioning using the native support of Spring Framework 7.
 *
 * The client picks the version in one of two ways (the first one found wins):
 *   - header:          API-Version: 2
 *   - query parameter: /employees?api-version=2
 *
 * Without a version the request goes to v1, so old clients keep working unchanged.
 * Controllers choose which version each endpoint serves with @GetMapping(path = "...", version = "2").
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    public static final String HEADER = "API-Version";
    public static final String QUERY_PARAM = "api-version";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        // v1 is still served, but every v1 response now carries the Deprecation and Sunset headers (RFC 9745 / RFC 8594)
        StandardApiVersionDeprecationHandler deprecation = new StandardApiVersionDeprecationHandler();
        deprecation.configureVersion("1")
                .setDeprecationDate(ZonedDateTime.of(2026, 10, 27, 0, 0, 0, 0, ZoneOffset.UTC))
                .setSunsetDate(ZonedDateTime.of(2027, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC))
                // Unversioned endpoints (home, swagger) also resolve to the default version; only warn on the employees API
                .setRequestPredicate(request -> request.getRequestURI().startsWith("/employee"));

        configurer
                .useRequestHeader(HEADER)
                .useQueryParam(QUERY_PARAM)
                .setDefaultVersion("1")
                .setDeprecationHandler(deprecation);
        // The supported versions (1 and 2) are detected from the "version" attribute of the mappings;
        // asking for any other version returns 400 Bad Request
    }
}
