package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.comment.CommentResponseDTO;
import com.nakamahub.backend.dtos.comment.CreateCommentDTO;
import com.nakamahub.backend.dtos.comment.UpdateCommentDTO;
import com.nakamahub.backend.models.Comment;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostVisibility postVisibility;
    private final BlockService blockService;
    private final CommentMapper commentMapper;

    public CommentServiceImpl(UserRepository userRepository,
                              CommentRepository commentRepository,
                              PostRepository postRepository,
                              PostVisibility postVisibility,
                              BlockService blockService,
                              CommentMapper commentMapper) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.postVisibility = postVisibility;
        this.blockService = blockService;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentResponseDTO createComment(CreateCommentDTO createCommentDTO, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Post post = postRepository.findWithAuthorById(createCommentDTO.getPostId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        // Sin esto se podía comentar en un borrador ajeno conociendo su id.
        requireVisible(post, author);

        Comment newComment = new Comment();
        newComment.setContent(createCommentDTO.getContent());
        newComment.setAuthor(author);
        newComment.setPost(post);

        if (createCommentDTO.getParentId() != null) {
            Comment parent = commentRepository.findById(createCommentDTO.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Comentario padre no encontrado"));

            if (!parent.getPost().getId().equals(post.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El comentario padre pertenece a otro post");
            }

            newComment.setParent(parent);
        }

        author.setReputationPoints(author.getReputationPoints() + 1);

        return commentMapper.toDTO(commentRepository.save(newComment), author);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getCommentsByPost(Long postId, Pageable pageable, String viewerUsername) {
        User viewer = findViewer(viewerUsername);
        requireVisiblePost(postId, viewerUsername);

        return commentMapper.toPage(
                commentRepository.findThreadStarters(postId, idOf(viewer), pageable), viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getCommentsByParent(Long parentId, Pageable pageable, String viewerUsername) {
        Comment parent = commentRepository.findWithAuthorById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        User viewer = findViewer(viewerUsername);
        requireVisible(parent.getPost(), viewer);

        return commentMapper.toPage(commentRepository.findReplies(parentId, idOf(viewer), pageable), viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDTO> getCommentsByUser(Long authorId, Pageable pageable, String viewerUsername) {
        User viewer = findViewer(viewerUsername);

        return commentMapper.toPage(
                commentRepository.findVisibleByAuthorId(authorId, idOf(viewer), pageable), viewer);
    }

    @Override
    public CommentResponseDTO updateComment(Long commentId, UpdateCommentDTO updateCommentDTO, String authorUsername) {
        Comment comment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        if (!comment.getAuthor().getUsername().equals(authorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes editar este comentario");
        }

        comment.setContent(updateCommentDTO.getContent());
        comment.setEditedAt(LocalDateTime.now());

        return commentMapper.toDTO(comment, comment.getAuthor());
    }

    @Override
    public void deleteComment(Long commentId, String authorUsername) {
        Comment targetComment = commentRepository.findWithAuthorById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));

        if (!targetComment.getAuthor().getUsername().equals(authorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes borrar este comentario");
        }

        commentRepository.delete(targetComment);
    }

    @Override
    public void deleteCommentAsAuthority(Long id) {
        Comment targetComment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));
        commentRepository.delete(targetComment);
    }

    private Long idOf(User user) {
        return user == null ? null : user.getId();
    }

    private User findViewer(String viewerUsername) {
        return viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).orElse(null);
    }

    private void requireVisiblePost(Long postId, String viewerUsername) {
        Post post = postRepository.findWithAuthorById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        requireVisible(post, findViewer(viewerUsername));
    }

    /** Mismo 404 que un post inexistente: un 403 confirmaría que el post existe. */
    private void requireVisible(Post post, User viewer) {
        if (!postVisibility.isVisibleTo(post, viewer)
                || blockService.blockedBetween(viewer, post.getAuthor())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }
    }
}
