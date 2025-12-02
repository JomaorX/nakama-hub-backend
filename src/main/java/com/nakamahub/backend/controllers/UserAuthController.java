package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.user.CreateUserDTO;
import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Tag(name = "Autenticación", description = "Endpoints para registro y login de usuarios")
@RestController
@RequestMapping("/auth")
public class UserAuthController {

    @Autowired
    UserService userService;

    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea una nueva cuenta de usuario con los datos proporcionados"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existente")
    })

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponseDTO signup (@Valid @RequestBody CreateUserDTO createUserDTO){
        return userService.registerUser(createUserDTO);
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario y devuelve un token JWT para futuras peticiones"
    )
    @RequestBody( // <-- Swagger RequestBody para documentar
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginUserDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "Login con username",
                                    value = "{ \"identifier\": \"nakama123\", \"password\": \"Password123\" }"
                            ),
                            @ExampleObject(
                                    name = "Login con email",
                                    value = "{ \"identifier\": \"nakama@example.com\", \"password\": \"Password123\" }"
                            )
                    }
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login exitoso, token devuelto"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO login (@Valid @RequestBody LoginUserDTO loginUserDTO){
        return  userService.authenticateUser(loginUserDTO);
    }
}
