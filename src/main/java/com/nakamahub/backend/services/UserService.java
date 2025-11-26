package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.*;

public interface UserService {

    SignupResponseDTO registerUser (CreateUserDTO createUserDTO);
    LoginResponseDTO authenticateUser (LoginUserDTO loginUserDTO);
    void toggleFollow(String followerUsername, String targetUsername);
    UserProfileDTO getProfile (String targetUsername, String viewerUsername);

}
