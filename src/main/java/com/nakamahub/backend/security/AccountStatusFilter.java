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
 * Contrasta el token con el estado real de la cuenta. Se ejecuta después del filtro
 * JWT, de modo que solo entra en juego cuando ya hay un usuario autenticado.
 *
 * Rechaza el token si la cuenta ya no existe o si su nombre de usuario ha cambiado
 * desde que se emitió, y corta la petición si la cuenta no está activa.
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

        Optional<String> username = currentUsername();

        if (username.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Long tokenUserId = (Long) request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        Optional<User> account = userRepository.findByUsername(username.get());

        // El nombre del token ya no corresponde a la misma cuenta: renombrada, borrada,
        // o el nombre lo ocupa ahora otra persona. En los tres casos el token está muerto.
        if (account.isEmpty() || tokenUserId == null || !tokenUserId.equals(account.get().getId())) {
            SecurityContextHolder.clearContext();
            errorResponder.write(request, response, HttpStatus.UNAUTHORIZED,
                    "Tu sesión ya no es válida, vuelve a iniciar sesión");
            return;
        }

        AccountStatus status = account.get().getStatus();
        if (status != AccountStatus.ACTIVE) {
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
