package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.UserRepository;
import com.nakamahub.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    PostVisibility postVisibility;

    @Autowired
    PostMapper postMapper;

    @Autowired
    CommentMapper commentMapper;

    @Autowired
    CommentRepository commentRepository;

    /** Prefijo reservado para las cuentas anonimizadas. Nadie puede registrarse con él. */
    static final String DELETED_USERNAME_PREFIX = "usuario_eliminado_";

    @Override
    public SignupResponseDTO registerUser(CreateUserDTO createUserDTO) {
        requireAvailableUsername(createUserDTO.getUsername());

        if (userRepository.existsByEmail(createUserDTO.getEmail()) || userRepository.existsByUsername(createUserDTO.getUsername())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credenciales inválidas o Ya existe un usuario con esos datos");
        }

        User newUser = new User();

        newUser.setEmail(createUserDTO.getEmail());
        newUser.setUsername(createUserDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(createUserDTO.getPassword()));

        User savedUser = userRepository.save(newUser);

        return SignupResponseDTO.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();
    }

    public LoginResponseDTO authenticateUser(LoginUserDTO loginUser) {
        String identifier = loginUser.getIdentifier();

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier);

        User user = userOpt.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos")
        );

        if (!passwordEncoder.matches(loginUser.getPassword(), user.getPassword())){
            throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    user.getStatus() == AccountStatus.SUSPENDED
                            ? "Tu cuenta está suspendida"
                            : "Tu cuenta ha sido eliminada");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return LoginResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }

    @Override
    public void toggleFollow(String followerUsername, String targetUsername) {
        if (followerUsername.equals(targetUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes seguirte a ti mismo");
        }

        User follower = userRepository.findByUsername(followerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower no encontrado"));

        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario objetivo no encontrado"));

        if (target.getFollowers().contains(follower)) {
            // Unfollow
            target.getFollowers().remove(follower);
            follower.getFollowing().remove(target);
            target.setReputationPoints(target.getReputationPoints() - 1);
        } else {
            // Follow
            target.getFollowers().add(follower);
            follower.getFollowing().add(target);
            target.setReputationPoints(target.getReputationPoints() + 1);
        }

        userRepository.save(target);
        userRepository.save(follower);
    }


    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return getUserProfileDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDTO getProfile(String targetUsername, String viewerUsername) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        // El endpoint es público, así que el visitante puede no existir. Antes se
        // buscaba "anonymousUser" en base de datos y el perfil devolvía un 404.
        User viewer = viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).orElse(null);

        boolean isOwner = viewer != null && target.equals(viewer);
        boolean isFollower = viewer != null && target.getFollowers().contains(viewer);

        if (target.getPrivacy() == ProfilePrivacy.PRIVATE && !isOwner && !isFollower) {
            // Perfil privado → solo datos básicos
            return UserPublicProfileDTO.builder()
                    .id(target.getId())
                    .username(target.getUsername())
                    .avatarUrl(target.getAvatarUrl())
                    .bio(target.getBio())
                    .build();
        }

        List<PostResponseDTO> visiblePosts = target.getPosts().stream()
                .filter(post -> postVisibility.isVisibleTo(post, isOwner, isFollower))
                .map(postMapper::toDTO)
                .toList();

        return UserPublicProfileDTO.builder()
                .id(target.getId())
                .username(target.getUsername())
                .bio(target.getBio())
                .avatarUrl(target.getAvatarUrl())
                .followersCount(target.getFollowers().size())
                .followingCount(target.getFollowing().size())
                .postsCount(visiblePosts.size())
                .posts(visiblePosts)
                .build();
    }

    @Override
    public UserProfileDTO updateUsername(String currentUsername, UpdateUsernameDTO dto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            requireAvailableUsername(dto.getUsername());
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nombre de usuario ya en uso");
            }
            user.setUsername(dto.getUsername());
        }

        userRepository.save(user);

        return getMe(user.getUsername());

    }

    @Override
    public UserProfileDTO updateEmail(String currentUsername, UpdateEmailDTO dto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya en uso");
            }
            user.setEmail(dto.getEmail());
        }

        userRepository.save(user);

        return getMe(user.getUsername());

    }

    @Override
    public UserProfileDTO updateBio(String currentUsername, UpdateBioDTO dto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.getBio() != null) user.setBio(dto.getBio());

        userRepository.save(user);

        return getMe(user.getUsername());

    }

    @Override
    public void suspendAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        user.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);
    }

    @Override
    public void deleteAccount(String username) {
        anonymize(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")));
    }

    @Override
    public void suspendUserAsAuthority(String username) {
        User targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        targetUser.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(targetUser);
    }

    @Override
    public void deleteAccountAsAuthority(String username) {
        anonymize(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")));
    }

    /**
     * Borrado lógico con anonimización, el modelo habitual en foros.
     *
     * El borrado físico no era viable: Comment.author es nullable = false y no había
     * cascada, así que cualquier cuenta que hubiera comentado en un post ajeno hacía
     * saltar la clave foránea. Y aunque funcionase, arrastraría los hilos de
     * conversación de terceros.
     *
     * Se eliminan los datos personales y se rompe el vínculo con la identidad, que es
     * lo que exige el derecho de supresión. Las publicaciones quedan atribuidas a una
     * cuenta sin datos, igual que hacen Reddit o Discourse.
     */
    private void anonymize(User user) {
        if (user.getStatus() == AccountStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está eliminada");
        }

        detachRelationships(user);

        user.setUsername(DELETED_USERNAME_PREFIX + user.getId());
        user.setEmail("eliminado-" + user.getId() + "@nakamahub.invalid");
        // Contraseña imposible de adivinar: la cuenta no puede volver a usarse.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setPrivacy(ProfilePrivacy.PRIVATE);
        user.setStatus(AccountStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * Deshace seguimientos y likes, devolviendo los puntos de reputación que esas
     * acciones habían otorgado. Si no, al borrarse una cuenta la reputación que repartió
     * quedaría inflada para siempre.
     */
    private void detachRelationships(User user) {
        user.getFollowers().clear();

        for (User followed : new HashSet<>(user.getFollowing())) {
            followed.getFollowers().remove(user);
            followed.setReputationPoints(Math.max(0, followed.getReputationPoints() - 1));
        }
        user.getFollowing().clear();

        for (Post likedPost : new HashSet<>(user.getLikedPosts())) {
            likedPost.setLikesCount(Math.max(0, likedPost.getLikesCount() - 1));
            User author = likedPost.getAuthor();
            author.setReputationPoints(Math.max(0, author.getReputationPoints() - 1));
        }
        user.getLikedPosts().clear();
    }

    private void requireAvailableUsername(String username) {
        if (username != null && username.toLowerCase().startsWith(DELETED_USERNAME_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese nombre de usuario está reservado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDataExportDTO exportMyData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return UserDataExportDTO.builder()
                .generatedAt(Instant.now())
                .account(UserDataExportDTO.Account.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .bio(user.getBio())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .privacy(user.getPrivacy().name())
                        .status(user.getStatus().name())
                        .reputationPoints(user.getReputationPoints())
                        .build())
                .followers(user.getFollowers().stream().map(User::getUsername).sorted().toList())
                .following(user.getFollowing().stream().map(User::getUsername).sorted().toList())
                .likedPostTitles(user.getLikedPosts().stream().map(Post::getTitle).sorted().toList())
                .posts(user.getPosts().stream().map(postMapper::toDTO).toList())
                .comments(commentRepository.findByAuthorIdOrderByCreatedAtDesc(user.getId()).stream()
                        .map(commentMapper::toDTO).toList())
                .build();
    }

    @Override
    public UserProfileDTO updateAvatar(String currentUsername, UpdateAvatarDTO dto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());

        userRepository.save(user);

        return getMe(user.getUsername());

    }

    @Override
    public UserProfileDTO updatePrivacy(String username, ProfilePrivacy privacy) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setPrivacy(privacy);
        userRepository.save(user);
        return getMe(user.getUsername());
    }

    private UserProfileDTO getUserProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .role(String.valueOf(user.getRole()))
                .followersCount(user.getFollowers().size())
                .followingCount(user.getFollowing().size())
                .reputationPoints(user.getReputationPoints())
                .postsCount(user.getPosts().size())
                .posts(user.getPosts().stream().map(postMapper::toDTO).toList())
                .build();
    }
}
