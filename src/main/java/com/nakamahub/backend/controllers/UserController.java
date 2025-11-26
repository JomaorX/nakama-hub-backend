package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.models.ProfilePrivacy;
import com.nakamahub.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    UserService userService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO getMe (){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getMe(username);
    }

    @GetMapping("/{username}")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicProfileDTO getUserProfile (@PathVariable String username) {
        String viewer = SecurityContextHolder.getContext().getAuthentication().getName();

        return userService.getProfile(username, viewer);
    }

    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicProfileDTO toggleFollow (@PathVariable String username) {
        String follower = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.toggleFollow(follower, username);
        return userService.getProfile(username, follower);
    }

    @PutMapping("/me/username")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateUsername(@RequestBody UpdateUsernameDTO dto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateUsername(currentUsername, dto);
    }

    @PutMapping("/me/email")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateEmail(@RequestBody UpdateEmailDTO dto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateEmail(currentUsername, dto);
    }

    @PutMapping("/me/bio")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateBio(@RequestBody UpdateBioDTO dto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateBio(currentUsername, dto);
    }

    @PutMapping("/me/avatar")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updateAvatar(@RequestBody UpdateAvatarDTO dto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateAvatar(currentUsername, dto);
    }

    @PutMapping("/me/privacy")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updatePrivacy(@RequestBody UpdatePrivacyDTO dto) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        ProfilePrivacy privacy = ProfilePrivacy.valueOf(dto.getPrivacy().toUpperCase());
        return userService.updatePrivacy(currentUsername, privacy);
    }

}
