package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.post.UpdatePostDTO;
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

import java.time.LocalDateTime;
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
    private final BlockService blockService;
    private final PostMapper postMapper;

    public PostServiceImpl(PostRepository postRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           SerieRepository serieRepository,
                           PostVisibility postVisibility,
                           BlockService blockService,
                           PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.serieRepository = serieRepository;
        this.postVisibility = postVisibility;
        this.blockService = blockService;
        this.postMapper = postMapper;
    }

    @Override
    public PostResponseDTO createPost(CreatePostDTO createPostDTO, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Serie postSerie = resolveSerie(createPostDTO.getSerieName());
        requireCoherentContentType(createPostDTO.getContentType(), postSerie);

        // Título duplicado para el mismo autor
        if (postRepository.existsByTitleAndAuthor(createPostDTO.getTitle(), author)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya posees un Post con ese título");
        }

        Post newPost = new Post();
        newPost.setTitle(createPostDTO.getTitle());
        newPost.setContent(createPostDTO.getContent());
        newPost.setContentType(createPostDTO.getContentType());
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

        return postMapper.toDTO(postRepository.save(newPost), author);
    }

    @Override
    public PostResponseDTO updatePost(Long id, UpdatePostDTO updatePostDTO, String username) {
        Post post = postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        if (!post.getAuthor().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes editar este post");
        }

        Serie postSerie = resolveSerie(updatePostDTO.getSerieName());
        requireCoherentContentType(updatePostDTO.getContentType(), postSerie);

        // Se excluye el propio post de la comprobación, si no editarlo sin cambiar el
        // título chocaría siempre contra la restricción de unicidad por autor.
        if (postRepository.existsByTitleAndAuthorAndIdNot(updatePostDTO.getTitle(), post.getAuthor(), post.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya posees otro Post con ese título");
        }

        post.setTitle(updatePostDTO.getTitle());
        post.setContent(updatePostDTO.getContent());
        post.setContentType(updatePostDTO.getContentType());
        post.setSerie(postSerie);
        post.setCategories(resolveCategories(updatePostDTO.getCategories()));

        if (updatePostDTO.getStatus() != null) {
            post.setStatus(updatePostDTO.getStatus());
        }
        if (updatePostDTO.getPrivacy() != null) {
            post.setPrivacy(updatePostDTO.getPrivacy());
        }

        // Se reemplaza el contenido de la colección en lugar de sustituirla, porque
        // Hibernate no admite que se le cambie la referencia de una colección gestionada.
        post.getImageUrls().clear();
        if (updatePostDTO.getImageUrls() != null) {
            post.getImageUrls().addAll(updatePostDTO.getImageUrls());
        }

        post.setEditedAt(LocalDateTime.now());

        // Editar no da reputación: si no, bastaría con reescribir un post para farmear puntos.
        return postMapper.toDTO(post, post.getAuthor());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getFollowingFeed(Pageable pageable, String viewerUsername) {
        User viewer = userRepository.findByUsername(viewerUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return postMapper.toPage(postRepository.findFollowingFeed(viewer.getId(), pageable), viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getAllPost(Pageable pageable, String viewerUsername) {
        User viewer = viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).orElse(null);
        Long viewerId = viewer == null ? null : viewer.getId();

        return postMapper.toPage(postRepository.findVisibleFor(viewerId, pageable), viewer);
    }

    @Override
    public PostResponseDTO getPostById(Long id, String viewerUsername) {
        Post post = postRepository.findWithAuthorById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        User viewer = viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).orElse(null);

        boolean isAuthor = viewer != null && post.getAuthor().equals(viewer);

        if (!postVisibility.isVisibleTo(post, viewer) || blockService.blockedBetween(viewer, post.getAuthor())) {
            // Mismo 404 que un post inexistente: un 403 confirmaría que el post existe.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado");
        }

        PostResponseDTO dto = postMapper.toDTO(post, viewer);

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

        if (!postVisibility.isVisibleTo(post, user) || blockService.blockedBetween(user, post.getAuthor())) {
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
        return postMapper.toDTO(post, user);
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

    private Serie resolveSerie(String serieName) {
        if (serieName == null || serieName.isBlank()) {
            return null;
        }
        return serieRepository.findByName(serieName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serie no encontrada"));
    }

    /** Un post asociado a una serie no puede ser GENERAL, y uno sin serie tiene que serlo. */
    private void requireCoherentContentType(ContentType type, Serie serie) {
        if (serie != null) {
            if (type == null || type == ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Si hay serie, el tipo de contenido debe ser ANIME, MANGA o SERIE");
            }
        } else if (type != ContentType.GENERAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Si no hay serie, el tipo de contenido debe ser GENERAL");
        }
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
