package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository <Post, Long> {
    List<Post> findByTitleContainingIgnoreCase(String title);
    boolean existsByTitleAndAuthor(String title, User author);

}
