package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.SerieRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SerieRepository serieRepository;
    private final PostVisibility postVisibility;
    private final PostMapper postMapper;

    public PostServiceImpl(PostRepository postRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           SerieRepository serieRepository,
                           PostVisibility postVisibility,
                           PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.serieRepository = serieRepository;
        this.postVisibility = postVisibility;
        this.postMapper = postMapper;
    }

    @Override
    public PostResponseDTO createPost(CreatePostDTO createPostDTO, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        ContentType type = createPostDTO.getContentType();
        String serieName = createPostDTO.getSerieName();
        boolean hasSerieName = serieName != null && !serieName.isBlank();

        Serie postSerie = null;
        if (hasSerieName) {
            postSerie = serieRepository.findByName(serieName)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serie no encontrada"));
        }

        // Validaciones de tipo de contenido según presencia de serie
        if (postSerie != null) {
            if (type == null || type == ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Si hay serie, el tipo de contenido debe ser ANIME, MANGA o SERIE");
            }
        } else if (type != ContentType.GENERAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Si no hay serie, el tipo de contenido debe ser GENERAL");
        }

        // Título duplicado para el mismo autor
        if (postRepository.existsByTitleAndAuthor(createPostDTO.getTitle(), author)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya posees un Post con ese título");
        }

        Post newPost = new Post();
        newPost.setTitle(createPostDTO.getTitle());
        newPost.setContent(createPostDTO.getContent());
        newPost.setContentType(type);
        newPost.setStatus(createPostDTO.getStatus() != null ? createPostDTO.getStatus() : PostStatus.DRAFT);
        newPost.setPrivacy(createPostDTO.getPrivacy() != null ? createPostDTO.getPrivacy() : PrivacyLevel.PUBLIC);
        newPost.setSerie(postSerie);
        newPost.setCategories(resolveCategories(createPostDTO.getCategories()));
        newPost.setAuthor(author);

        if (createPostDTO.getImageUrls() != null) {
            newPost.setImageUrls(createPostDTO.getImageUrls());
        }

        author.setReputationPoints(author.getReputationPoints() + 1);
        userRepository.save(author);

        return postMapper.toDTO(postRepository.save(newPost));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getAllPost(Pageable pageable, String viewerUsername) {
        Long viewerId = userRepository.findByUsername(viewerUsername == null ? "" : viewerUsername)
                .map(User::getId)
                .orElse(null);

        return postRepository.findVisibleFor(viewerId, pageable).map(postMapper::toDTO);
    }

    @Override
    public PostResponseDTO getPostById(Long id, String viewerUsername) {
        Post post = postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        User viewer = viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).orElse(null);

        boolean isAuthor = viewer != null && post.getAuthor().equals(viewer);

        if (!postVisibility.isVisibleTo(post, viewer)) {
            // Mismo 404 que un post inexistente: un 403 confirmaría que el post existe.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }

        PostResponseDTO dto = postMapper.toDTO(post);

        if (!isAuthor) {
            postRepository.incrementViewsCount(id);
            dto.setViewsCount(dto.getViewsCount() + 1);
        }

        return dto;
    }

    @Override
    public PostResponseDTO toggleLike(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Post post = postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        if (!postVisibility.isVisibleTo(post, user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }

        User author = post.getAuthor();

        if (user.getLikedPosts().remove(post)) {
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            author.setReputationPoints(author.getReputationPoints() - 1);
        } else {
            user.getLikedPosts().add(post);
            post.setLikesCount(post.getLikesCount() + 1);
            author.setReputationPoints(author.getReputationPoints() + 1);
        }

        // Las tres escrituras van en la misma transacción del método, así que un fallo
        // a mitad ya no deja el contador de likes desacompasado de la reputación.
        return postMapper.toDTO(post);
    }

    @Override
    public void deletePost(Long id, String currentUsername) {
        Post targetPost = postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        if (!targetPost.getAuthor().getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes borrar este post");
        }

        postRepository.delete(targetPost);
    }

    @Override
    public void deletePostAsAuthority(Long id) {
        Post targetPost = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        postRepository.delete(targetPost);
    }

    private Set<Category> resolveCategories(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar al menos una categoría");
        }
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Categoría no válida: " + name)))
                .collect(Collectors.toSet());
    }
}
