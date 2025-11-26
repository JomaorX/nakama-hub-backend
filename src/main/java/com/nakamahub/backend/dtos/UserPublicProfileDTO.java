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
public class UserPublicProfileDTO {
    private Long id;
    private String username;
    private String bio;
    private String avatarUrl;

    private int followersCount;
    private int followingCount;
    private int postsCount;

    private List<PostResponseDTO> posts;
}
