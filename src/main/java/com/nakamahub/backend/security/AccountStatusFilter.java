package com.nakamahub.backend.security;

import com.nakamahub.backend.models.AccountStatus;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Corta las peticiones de cuentas suspendidas o borradas. Se ejecuta después del
 * filtro JWT, de modo que solo entra en juego cuando ya hay un usuario autenticado.
 */
@Component
public class AccountStatusFilter extends OncePerRequestFilter {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private final UserRepository userRepository;
    private final HttpErrorResponder errorResponder;

    public AccountStatusFilter(UserRepository userRepository, HttpErrorResponder errorResponder) {
        this.userRepository = userRepository;
        this.errorResponder = errorResponder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Optional<User> authenticatedUser = currentUsername()
                .flatMap(userRepository::findByUsername);

        if (authenticatedUser.isPresent() && authenticatedUser.get().getStatus() != AccountStatus.ACTIVE) {
            AccountStatus status = authenticatedUser.get().getStatus();
            SecurityContextHolder.clearContext();
            errorResponder.write(request, response, HttpStatus.FORBIDDEN,
                    status == AccountStatus.SUSPENDED
                            ? "Tu cuenta está suspendida"
                            : "Tu cuenta ha sido eliminada");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || ANONYMOUS_USER.equals(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
