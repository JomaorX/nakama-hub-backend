package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.models.ProfilePrivacy;

public interface UserService {

    SignupResponseDTO registerUser (CreateUserDTO createUserDTO);
    LoginResponseDTO authenticateUser (LoginUserDTO loginUserDTO);

    /** Canjea un token de refresco por un par nuevo de tokens. */
    LoginResponseDTO refreshSession (String refreshToken);

    /** Cierra la sesión asociada a ese token de refresco. */
    void logout (String refreshToken);

    /** Cambia la contraseña y cierra todas las sesiones abiertas. */
    void changePassword (String username, ChangePasswordDTO dto);

    /** Envía el enlace de restablecimiento. No revela si la dirección existe. */
    void requestPasswordReset (String email);

    /** Canjea el enlace, fija la contraseña nueva y cierra las sesiones abiertas. */
    void resetPassword (String token, String newPassword);

    /** Confirma la dirección de correo con el token del enlace. */
    void verifyEmail (String token);

    /** Vuelve a enviar el correo de verificación al usuario indicado. */
    void resendVerification (String username);
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
    void deleteAccountAsAuthority (String username);

    /** Volcado completo de los datos personales del propio usuario (RGPD, derecho de acceso). */
    UserDataExportDTO exportMyData (String username);
}
