package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreateUserDTO;
import com.nakamahub.backend.dtos.LoginResponseDTO;
import com.nakamahub.backend.dtos.SingupResponseDTO;

public interface UserService {

    SingupResponseDTO registerUser (CreateUserDTO createUserDTO);
    LoginResponseDTO authenticateUser (LoginResponseDTO loginResponseDTO);

    boolean isEmail(String identifier);
}
