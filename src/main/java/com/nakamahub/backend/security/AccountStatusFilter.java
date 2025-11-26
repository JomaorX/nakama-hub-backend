package com.nakamahub.backend.security;

import com.nakamahub.backend.models.AccountStatus;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

public class AccountStatusFilter extends OncePerRequestFilter {
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && user.getStatus() != AccountStatus.ACTIVE) {
                // Bloquea la petición si la cuenta no está activa
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta no activa");
            }
        }

        filterChain.doFilter(request, response);
    }
}
