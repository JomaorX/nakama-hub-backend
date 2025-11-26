package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.UserProfileDTO;
import com.nakamahub.backend.dtos.UserPublicProfileDTO;
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

    @GetMapping("/me/privacy")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileDTO updatePrivacy(@RequestBody String body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ProfilePrivacy privacy = ProfilePrivacy.valueOf(body.toUpperCase());
        return userService.updatePrivacy(username, privacy);
    }
}
