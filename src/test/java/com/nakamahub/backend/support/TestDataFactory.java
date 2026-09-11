package com.nakamahub.backend.support;

import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.RefreshTokenRepository;
import com.nakamahub.backend.repositories.ReportRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
import com.nakamahub.backend.security.JwtUtil;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@TestComponent
public class TestDataFactory {

    public static final String PASSWORD = "Secreta1";

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public TestDataFactory(UserRepository userRepository,
                           PostRepository postRepository,
                           CategoryRepository categoryRepository,
                           CommentRepository commentRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           ReportRepository reportRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User user(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@nakamahub.test");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User user(String username) {
        return user(username, UserRole.ROLE_USER);
    }

    @Transactional
    public Post post(User author, String title, PostStatus status, PrivacyLevel privacy) {
        Category category = categoryRepository.findByName("General").orElseThrow();

        Post post = new Post();
        post.setTitle(title);
        post.setContent("Contenido de prueba para " + title);
        post.setAuthor(author);
        post.setContentType(ContentType.GENERAL);
        post.setStatus(status);
        post.setPrivacy(privacy);
        post.setCategories(Set.of(category));
        return postRepository.save(post);
    }

    @Transactional
    public Comment comment(User author, Post post, String content) {
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public void like(User user, Post post) {
        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        Post managedPost = postRepository.findById(post.getId()).orElseThrow();
        managedUser.getLikedPosts().add(managedPost);
        managedPost.setLikesCount(managedPost.getLikesCount() + 1);
        managedPost.getAuthor().setReputationPoints(managedPost.getAuthor().getReputationPoints() + 1);
        userRepository.save(managedUser);
        postRepository.save(managedPost);
    }

    @Transactional
    public void follow(User follower, User target) {
        User managedTarget = userRepository.findById(target.getId()).orElseThrow();
        User managedFollower = userRepository.findById(follower.getId()).orElseThrow();
        managedTarget.getFollowers().add(managedFollower);
        managedTarget.setReputationPoints(managedTarget.getReputationPoints() + 1);
        userRepository.save(managedTarget);
    }

    /** Cabecera Authorization para el usuario indicado, con su identificador real. */
    @Transactional(readOnly = true)
    public String bearer(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return "Bearer " + jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void clear() {
        // Los reportes no se cascadean al borrar un usuario a propósito: sobreviven
        // como historial de moderación. Aquí hay que limpiarlos explícitamente.
        reportRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }
}
