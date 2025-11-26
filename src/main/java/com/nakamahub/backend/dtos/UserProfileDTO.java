package com.nakamahub.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private String role;

    private int followersCount;
    private int followingCount;
    private int postsCount;

    private List<String> followers;
    private List<String> following;
    private List<PostResponseDTO> posts;
}
