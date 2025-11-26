package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreateUserDTO;
import com.nakamahub.backend.dtos.LoginResponseDTO;
import com.nakamahub.backend.dtos.LoginUserDTO;
import com.nakamahub.backend.dtos.SignupResponseDTO;

public interface UserService {

    SignupResponseDTO registerUser (CreateUserDTO createUserDTO);
    LoginResponseDTO authenticateUser (LoginUserDTO loginUserDTO);
    void toggleFollow(String followerUsername, String targetUsername);
}
