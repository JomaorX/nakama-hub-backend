package com.nakamahub.backend.config;

import com.nakamahub.backend.security.AccountStatusFilter;
import com.nakamahub.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private AccountStatusFilter accountStatusFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Esto ahora sí funcionará
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                                // Endpoints públicos
                                .requestMatchers("/auth/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/users/{username}").permitAll()

                                // Endpoints que requieren autenticación
                                .requestMatchers("/api/users/me/**").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/posts/**").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/users/{username}/follow").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/users/me/**").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/users/me").hasAnyAuthority("ROLE_USER","ROLE_MODERATOR","ROLE_ADMIN")

                                // Endpoints con permisos especiales
                                .requestMatchers(HttpMethod.PUT, "/api/users/*/suspend").hasAnyAuthority("ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/posts/*/authority").hasAnyAuthority("ROLE_MODERATOR","ROLE_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/comments/*/authority").hasAnyAuthority("ROLE_MODERATOR","ROLE_ADMIN")

                        // Tod0 lo demás requiere estar autenticado
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(accountStatusFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
