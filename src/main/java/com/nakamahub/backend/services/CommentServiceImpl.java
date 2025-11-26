package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.dtos.comment.CreateCommentDTO;
import com.nakamahub.backend.models.Comment;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


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

            newComment.setParent(parent);
        }

        author.setReputationPoints(author.getReputationPoints() + 1);
        userRepository.save(author);
        Comment savedComment = commentRepository.save(newComment);

        return CommentResponseDTO.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .postId(savedComment.getPost().getId())
                .authorUsername(savedComment.getAuthor().getUsername())
                .parentId(savedComment.getParent() != null ? savedComment.getParent().getId() : null)
                .createdAt(savedComment.getCreatedAt())
                .updatedAt(savedComment.getUpdatedAt())
                .build();
    }

    @Override
    public Page<CommentResponseDTO> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndParentIdIsNull(postId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<CommentResponseDTO> getCommentsByUser(Long authorId, Pageable pageable) {
        return commentRepository.findByAuthorId(authorId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<CommentResponseDTO> getCommentsByParent(Long parentId, Pageable pageable) {
        return commentRepository.findByParentId(parentId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public void deleteComment(Long commentId, String authorUsername) {
    Comment targetComment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comentario no encontrado"));

    if (!targetComment.getAuthor().getUsername().equals(authorUsername)){
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes borrar este comentario");
    }

    commentRepository.delete(targetComment);
    }


    private CommentResponseDTO mapToDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
