package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.CreateUserDTO;
import com.nakamahub.backend.dtos.SignupResponseDTO;
import com.nakamahub.backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    @Autowired
    UserService userService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponseDTO signup (@Valid @RequestBody CreateUserDTO createUserDTO){
        return userService.registerUser(createUserDTO);
    }
}
