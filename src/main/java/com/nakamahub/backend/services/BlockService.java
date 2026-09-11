package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.search.UserSearchResultDTO;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Bloqueo entre usuarios.
 *
 * Se guarda en un solo sentido, de quien bloquea a quien es bloqueado, pero surte
 * efecto en los dos: ninguno ve las publicaciones ni los comentarios del otro, no
 * pueden seguirse y el bloqueado deja de ver el perfil del que le bloqueó.
 */
@Service
@Transactional
public class BlockService {

    private final UserRepository userRepository;

    public BlockService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** @return true si ha quedado bloqueado, false si se ha desbloqueado. */
    public boolean toggleBlock(String blockerUsername, String targetUsername) {
        if (blockerUsername.equals(targetUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes bloquearte a ti mismo");
        }

        User blocker = require(blockerUsername);
        User target = require(targetUsername);

        if (blocker.getBlockedUsers().remove(target)) {
            return false;
        }

        blocker.getBlockedUsers().add(target);

        // Un bloqueo que deja el seguimiento en pie no sirve de nada: el bloqueado
        // seguiría apareciendo en la lista de seguidores y recibiría las novedades.
        removeFollow(blocker, target);
        removeFollow(target, blocker);

        return true;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResultDTO> listBlocked(String username) {
        return require(username).getBlockedUsers().stream()
                .map(blocked -> UserSearchResultDTO.builder()
                        .id(blocked.getId())
                        .username(blocked.getUsername())
                        .bio(blocked.getBio())
                        .avatarUrl(blocked.getAvatarUrl())
                        .followersCount(0)
                        .reputationPoints(0)
                        .build())
                .sorted((a, b) -> a.username().compareToIgnoreCase(b.username()))
                .toList();
    }

    /** Si cualquiera de los dos ha bloqueado al otro, no deben interactuar. */
    @Transactional(readOnly = true)
    public boolean blockedBetween(User one, User other) {
        if (one == null || other == null || one.equals(other)) {
            return false;
        }
        return one.getBlockedUsers().contains(other) || other.getBlockedUsers().contains(one);
    }

    private void removeFollow(User follower, User followed) {
        if (followed.getFollowers().remove(follower)) {
            follower.getFollowing().remove(followed);
            followed.setReputationPoints(Math.max(0, followed.getReputationPoints() - 1));
        }
    }

    private User require(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
