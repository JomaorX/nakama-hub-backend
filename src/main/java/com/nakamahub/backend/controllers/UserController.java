package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.security.SecurityUtils;
import com.nakamahub.backend.dtos.search.UserSearchResultDTO;
import com.nakamahub.backend.services.BlockService;
import com.nakamahub.backend.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final BlockService blockService;

    public UserController(UserService userService, BlockService blockService) {
        this.userService = userService;
        this.blockService = blockService;
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO getMe() {
        return userService.getMe(SecurityUtils.requireCurrentUsername());
    }

    @GetMapping("/{username}")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicProfileDTO getUserProfile(@PathVariable String username) {
        // Endpoint público: el visitante puede ser anónimo.
        return userService.getProfile(username, SecurityUtils.currentUsername().orElse(null));
    }

    @PutMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicProfileDTO toggleFollow(@PathVariable String username) {
        String follower = SecurityUtils.requireCurrentUsername();
        userService.toggleFollow(follower, username);
        return userService.getProfile(username, follower);
    }

    @PutMapping("/me/username")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateUsername(@Valid @RequestBody UpdateUsernameDTO dto) {
        return userService.updateUsername(SecurityUtils.requireCurrentUsername(), dto);
    }

    @PutMapping("/me/email")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateEmail(@Valid @RequestBody UpdateEmailDTO dto) {
        return userService.updateEmail(SecurityUtils.requireCurrentUsername(), dto);
    }

    @PutMapping("/me/bio")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateBio(@Valid @RequestBody UpdateBioDTO dto) {
        return userService.updateBio(SecurityUtils.requireCurrentUsername(), dto);
    }

    @PutMapping("/me/avatar")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateAvatar(@Valid @RequestBody UpdateAvatarDTO dto) {
        return userService.updateAvatar(SecurityUtils.requireCurrentUsername(), dto);
    }

    @PutMapping("/me/privacy")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updatePrivacy(@Valid @RequestBody UpdatePrivacyDTO dto) {
        return userService.updatePrivacy(SecurityUtils.requireCurrentUsername(), dto.getPrivacy());
    }

    /**
     * Bloquea o desbloquea a otra cuenta. Devuelve el perfil actualizado, salvo al
     * bloquear: a partir de ese momento deja de haber perfil que enseñar.
     */
    @PutMapping("/{username}/block")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicProfileDTO toggleBlock(@PathVariable String username) {
        String blocker = SecurityUtils.requireCurrentUsername();
        blockService.toggleBlock(blocker, username);
        return userService.getProfile(username, blocker);
    }

    @GetMapping("/me/blocked")
    @ResponseStatus(HttpStatus.OK)
    public List<UserSearchResultDTO> listBlocked() {
        return blockService.listBlocked(SecurityUtils.requireCurrentUsername());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(SecurityUtils.requireCurrentUsername(), dto);
    }

    @PostMapping("/me/verify/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification() {
        userService.resendVerification(SecurityUtils.requireCurrentUsername());
    }

    @GetMapping("/me/export")
    @ResponseStatus(HttpStatus.OK)
    public UserDataExportDTO exportMyData() {
        return userService.exportMyData(SecurityUtils.requireCurrentUsername());
    }

    /** Borrado lógico con anonimización: los datos personales se eliminan y la cuenta no vuelve a usarse. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount() {
        userService.deleteAccount(SecurityUtils.requireCurrentUsername());
    }

    @PutMapping("/me/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendAccount() {
        userService.suspendAccount(SecurityUtils.requireCurrentUsername());
    }

    @PutMapping("/{username}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendAccountAsAuthority(@PathVariable String username) {
        userService.suspendUserAsAuthority(username);
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountAsAuthority(@PathVariable String username) {
        userService.deleteAccountAsAuthority(username);
    }
}
