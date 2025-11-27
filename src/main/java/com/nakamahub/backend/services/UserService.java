package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.models.ProfilePrivacy;

public interface UserService {

    SignupResponseDTO registerUser (CreateUserDTO createUserDTO);
    LoginResponseDTO authenticateUser (LoginUserDTO loginUserDTO);
    void toggleFollow(String followerUsername, String targetUsername);
    UserProfileDTO getMe (String username);
    UserPublicProfileDTO getProfile (String targetUsername, String viewerUsername);
    UserProfileDTO updatePrivacy (String username, ProfilePrivacy privacy);
    UserProfileDTO updateUsername (String currentUsername, UpdateUsernameDTO dto);
    UserProfileDTO updateEmail (String currentUsername, UpdateEmailDTO dto);
    UserProfileDTO updateAvatar (String currentUsername, UpdateAvatarDTO dto);
    UserProfileDTO updateBio (String currentUsername, UpdateBioDTO dto);
    void suspendAccount (String username);
    void deleteAccount (String username);
    void suspendUserAsAuthority(String username);
}
