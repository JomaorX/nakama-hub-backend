package com.nakamahub.backend.config;

import com.nakamahub.backend.security.AccountStatusFilter;
import com.nakamahub.backend.security.HttpErrorResponder;
import com.nakamahub.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ROLES_AUTHENTICATED = {"ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN"};
    private static final String[] ROLES_STAFF = {"ROLE_MODERATOR", "ROLE_ADMIN"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AccountStatusFilter accountStatusFilter;
    private final HttpErrorResponder errorResponder;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AccountStatusFilter accountStatusFilter,
                          HttpErrorResponder errorResponder,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.accountStatusFilter = accountStatusFilter;
        this.errorResponder = errorResponder;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Sin cookies de sesión no hay superficie para CSRF: la API es stateless
                // y el token viaja en la cabecera Authorization.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        // Las rutas propias van antes que el comodín /api/users/{username},
                        // que también casa con el literal "me" y dejaría /api/users/me público.
                        .requestMatchers("/api/users/me", "/api/users/me/**").hasAnyAuthority(ROLES_AUTHENTICATED)

                        // Moderación
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/suspend").hasAnyAuthority(ROLES_STAFF)
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasAnyAuthority(ROLES_STAFF)
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*/authority").hasAnyAuthority(ROLES_STAFF)
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*/authority").hasAnyAuthority(ROLES_STAFF)

                        // El timeline es personal, así que va antes que la lectura pública de posts.
                        .requestMatchers(HttpMethod.GET, "/api/posts/feed").hasAnyAuthority(ROLES_AUTHENTICATED)

                        // Lectura pública
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()

                        // Escritura autenticada
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/follow").hasAnyAuthority(ROLES_AUTHENTICATED)
                        .requestMatchers(HttpMethod.POST, "/api/posts/**").hasAnyAuthority(ROLES_AUTHENTICATED)
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAnyAuthority(ROLES_AUTHENTICATED)
                        .requestMatchers(HttpMethod.PUT, "/api/posts/*").hasAnyAuthority(ROLES_AUTHENTICATED)
                        .requestMatchers(HttpMethod.PUT, "/api/comments/*").hasAnyAuthority(ROLES_AUTHENTICATED)

                        .anyRequest().authenticated()
                )
                // Sin esto Spring responde 403 a una petición sin token, cuando lo correcto
                // es 401: el cliente necesita distinguir "identifícate" de "no tienes permiso".
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                errorResponder.write(request, response, HttpStatus.UNAUTHORIZED,
                                        "Necesitas iniciar sesión para acceder a este recurso"))
                        .accessDeniedHandler((request, response, deniedException) ->
                                errorResponder.write(request, response, HttpStatus.FORBIDDEN,
                                        "No tienes permisos para realizar esta acción"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(accountStatusFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
