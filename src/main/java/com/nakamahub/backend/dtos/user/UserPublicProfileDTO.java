package com.nakamahub.backend.dtos.user;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
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

    /** Si quien consulta ya sigue a este usuario. False para visitantes anónimos. */
    private boolean followedByMe;

    /** Si el perfil es el de quien consulta. */
    private boolean own;

    /** Si quien consulta ha bloqueado a este usuario. */
    private boolean blockedByMe;
}
