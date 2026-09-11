package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.ForgotPasswordDTO;
import com.nakamahub.backend.dtos.auth.RefreshTokenRequestDTO;
import com.nakamahub.backend.dtos.auth.ResetPasswordDTO;
import com.nakamahub.backend.dtos.auth.TokenRequestDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.dtos.user.CreateUserDTO;
import com.nakamahub.backend.security.LoginAttemptPolicy;
import com.nakamahub.backend.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserAuthController {

    private final UserService userService;
    private final LoginAttemptPolicy attemptPolicy;

    public UserAuthController(UserService userService, LoginAttemptPolicy attemptPolicy) {
        this.userService = userService;
        this.attemptPolicy = attemptPolicy;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponseDTO signup(@Valid @RequestBody CreateUserDTO createUserDTO,
                                    HttpServletRequest request) {
        attemptPolicy.checkSignup(request);
        SignupResponseDTO created = userService.registerUser(createUserDTO);
        attemptPolicy.recordSignup(request);
        return created;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO login(@Valid @RequestBody LoginUserDTO loginUserDTO,
                                  HttpServletRequest request) {
        String identifier = loginUserDTO.getIdentifier();

        // Se comprueba antes de tocar la base de datos: si la cuenta está bloqueada
        // por intentos, ni siquiera merece la pena comparar la contraseña.
        attemptPolicy.checkLogin(identifier, request);

        try {
            LoginResponseDTO session = userService.authenticateUser(loginUserDTO);
            attemptPolicy.recordLoginSuccess(identifier, request);
            return session;
        } catch (RuntimeException ex) {
            attemptPolicy.recordLoginFailure(identifier, request);
            throw ex;
        }
    }

    /** Canjea el token de refresco por un par nuevo. El anterior queda invalidado. */
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO refresh(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return userService.refreshSession(dto.getRefreshToken());
    }

    /**
     * Pide el enlace de restablecimiento. Devuelve 204 exista o no la cuenta: si
     * distinguiera, el formulario serviría para averiguar qué correos están dados
     * de alta.
     */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto, HttpServletRequest request) {
        attemptPolicy.checkPasswordReset(dto.getEmail(), request);
        attemptPolicy.recordPasswordReset(dto.getEmail(), request);
        userService.requestPasswordReset(dto.getEmail());
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequestDTO dto) {
        userService.verifyEmail(dto.getToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        userService.logout(dto.getRefreshToken());
    }
}
