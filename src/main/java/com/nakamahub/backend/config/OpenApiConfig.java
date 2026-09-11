package com.nakamahub.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación viva de la API en /swagger-ui.html, generada del código.
 *
 * Una colección de Postman exportada a mano se queda desfasada en cuanto se toca un
 * endpoint. Esto además sirve un contrato OpenAPI en /v3/api-docs, del que se puede
 * generar directamente el cliente HTTP del frontend.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI nakamaHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nakama Hub API")
                        .version("v1")
                        .description("""
                                API de Nakama Hub, comunidad de anime, manga y series.

                                La autenticación usa un token de acceso JWT de vida corta que se envía en
                                la cabecera Authorization, y un token de refresco opaco de un solo uso que
                                se canjea en POST /auth/refresh.
                                """)
                        .contact(new Contact()
                                .name("José Miguel Martínez")
                                .url("https://jomaor.dev"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aquí el accessToken que devuelve POST /auth/login")));
    }
}
