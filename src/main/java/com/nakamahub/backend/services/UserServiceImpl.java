package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.*;
import com.nakamahub.backend.models.Category;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.ProfilePrivacy;
import com.nakamahub.backend.models.User;
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
        User follower = userRepository.findByUsername(followerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower no encontrado"));

        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario a seguir no encontrado"));

        if (follower.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes seguirte a ti mismo");
        }

        if (target.getFollowers().contains(follower)) {
            // Ya lo seguía → dejar de seguir
            target.getFollowers().remove(follower);
            follower.getFollowing().remove(target);
        } else {
            // No lo seguía → empezar a seguir
            target.getFollowers().add(follower);
            follower.getFollowing().add(target);
        }

        userRepository.save(follower);
        userRepository.save(target);
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
    public UserProfileDTO updatePrivacy(String username, ProfilePrivacy privacy) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setPrivacy(privacy);
        userRepository.save(user);
        return getUserProfileDTO(user);
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
