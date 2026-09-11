package com.nakamahub.backend.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractToken(request)
                    .flatMap(jwtUtil::parseToken)
                    .ifPresent(jwt -> authenticate(jwt, request));
        }

        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }

    /**
     * Solo autentica si el token trae sujeto y rol. Antes el filtro seguía adelante
     * aunque la verificación hubiera fallado y construía una autoridad nula, lo que
     * convertía cualquier token caducado o manipulado en un error 500.
     */
    private void authenticate(DecodedJWT jwt, HttpServletRequest request) {
        String username = jwt.getSubject();
        String role = jwt.getClaim(JwtUtil.ROLE_CLAIM).asString();

        if (username == null || username.isBlank() || role == null || role.isBlank()) {
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
