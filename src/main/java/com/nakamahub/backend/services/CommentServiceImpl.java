package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CommentResponseDTO;
import com.nakamahub.backend.dtos.CreateCommentDTO;
import com.nakamahub.backend.models.Comment;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PostRepository postRepository;

    @Override
    public CommentResponseDTO createComment(CreateCommentDTO createCommentDTO, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));

        Post post = postRepository.findById(createCommentDTO.getPostId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Post no encontrado"));

        if (createCommentDTO.getContent() == null || createCommentDTO.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido del comentario no puede estar vacío");
        }


        Comment newComment = new Comment();
        newComment.setContent(createCommentDTO.getContent());
        newComment.setAuthor(author);
        newComment.setPost(post);

        if (createCommentDTO.getParentId() != null) {
            Comment parent = commentRepository.findById(createCommentDTO.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comentario padre no encontrado"));

            if (!parent.getPost().getId().equals(post.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario padre pertenece a otro post");
            }

            newComment.setParentId(parent.getId());
        }

        Comment savedComment = commentRepository.save(newComment);

        return CommentResponseDTO.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .postId(savedComment.getPost().getId())
                .authorUsername(savedComment.getAuthor().getUsername())
                .parentId(savedComment.getParentId())
                .createdAt(savedComment.getCreatedAt())
                .updatedAt(savedComment.getUpdatedAt())
                .build();
    }

    @Override
    public List<CommentResponseDTO> getCommentsByPost(Long postId) {
        return List.of();
    }

    @Override
    public List<CommentResponseDTO> getCommentsByUser(Long authorId) {
        return List.of();
    }

    @Override
    public List<CommentResponseDTO> getCommentsByParent(Long parentId) {
        return List.of();
    }
}
