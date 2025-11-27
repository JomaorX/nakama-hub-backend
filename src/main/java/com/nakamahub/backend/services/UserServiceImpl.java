package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.auth.LoginResponseDTO;
import com.nakamahub.backend.dtos.auth.LoginUserDTO;
import com.nakamahub.backend.dtos.auth.SignupResponseDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.user.*;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.UserRepository;
import com.nakamahub.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

    @Override
    public SignupResponseDTO registerUser(CreateUserDTO createUserDTO) {
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

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

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
    public UserProfileDTO getMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return getUserProfileDTO(user);
    }

    @Override
    public UserPublicProfileDTO getProfile(String targetUsername, String viewerUsername) {
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil suspendido");
        }

        User viewer = userRepository.findByUsername(viewerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viewer no encontrado"));

        boolean isOwner = target.equals(viewer);
        boolean isFollower = target.getFollowers().contains(viewer);

        if (target.getPrivacy() == ProfilePrivacy.PRIVATE && !isOwner && !isFollower) {
            // Perfil privado → solo datos básicos
            return UserPublicProfileDTO.builder()
                    .id(target.getId())
                    .username(target.getUsername())
                    .avatarUrl(target.getAvatarUrl())
                    .bio(target.getBio())
                    .build();
        }

        // Perfil público o viewer autorizado → datos completos
        return UserPublicProfileDTO.builder()
                .id(target.getId())
                .username(target.getUsername())
                .bio(target.getBio())
                .avatarUrl(target.getAvatarUrl())
                .followersCount(target.getFollowers().size())
                .followingCount(target.getFollowing().size())
                .postsCount(target.getPosts().size())
                .posts(target.getPosts().stream().map(this::getPostResponseDTO).toList())
                .build();
    }


    @Override
    public UserProfileDTO updateUsername(String currentUsername, UpdateUsernameDTO dto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        userRepository.delete(user);
    }

    @Override
    public void suspendUserAsAuthority(String username) {
    User targetUser = userRepository.findByUsername(username)
            .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    targetUser.setStatus(AccountStatus.SUSPENDED);
    userRepository.save(targetUser);
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

    private PostResponseDTO getPostResponseDTO(Post post) {
        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categories(post.getCategories().stream().map(Category::getName).toList())
                .authorUsername(post.getAuthor().getUsername())
                .serieName(post.getSerie() != null ? post.getSerie().getName() : null)
                .contentType(post.getContentType())
                .imageUrls(post.getImageUrls())
                .status(post.getStatus())
                .privacy(post.getPrivacy())
                .viewsCount(post.getViewsCount())
                .likesCount(post.getLikesCount())
                .build();
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
                .posts(user.getPosts().stream().map(this::getPostResponseDTO).toList())
                .build();
    }
}
