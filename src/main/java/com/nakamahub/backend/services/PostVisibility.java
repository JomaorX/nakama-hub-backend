package com.nakamahub.backend.services;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.User;
import org.springframework.stereotype.Component;

/**
 * Única definición de quién puede ver un post.
 *
 * La consulta findVisibleFor del repositorio aplica estas mismas reglas en SQL para
 * el feed paginado. Cualquier cambio aquí tiene que replicarse allí.
 */
@Component
public class PostVisibility {

    public boolean isVisibleTo(Post post, User viewer) {
        boolean isAuthor = viewer != null && post.getAuthor().equals(viewer);
        boolean isFollower = viewer != null && post.getAuthor().getFollowers().contains(viewer);
        return isVisibleTo(post, isAuthor, isFollower);
    }

    public boolean isVisibleTo(Post post, boolean isAuthor, boolean isFollower) {
        if (isAuthor) {
            return true;
        }
        if (post.getStatus() != PostStatus.PUBLISHED) {
            return false;
        }
        return switch (post.getPrivacy()) {
            case PUBLIC -> true;
            case FOLLOWERS_ONLY -> isFollower;
            case PRIVATE -> false;
        };
    }
}
