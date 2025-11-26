package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.user.CreateUserDTO;
import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserAuthController {

    @Autowired
    UserService userService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponseDTO signup (@Valid @RequestBody CreateUserDTO createUserDTO){
        return userService.registerUser(createUserDTO);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDTO login (@Valid @RequestBody LoginUserDTO loginUserDTO){
        return  userService.authenticateUser(loginUserDTO);
    }
}
