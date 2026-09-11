package com.nakamahub.backend.support;

import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

    public TestDataFactory(UserRepository userRepository,
                           PostRepository postRepository,
                           CategoryRepository categoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
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
    public void follow(User follower, User target) {
        User managedTarget = userRepository.findById(target.getId()).orElseThrow();
        User managedFollower = userRepository.findById(follower.getId()).orElseThrow();
        managedTarget.getFollowers().add(managedFollower);
        userRepository.save(managedTarget);
    }

    @Transactional
    public void clear() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }
}
