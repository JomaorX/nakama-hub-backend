package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.CreatePostDTO;
import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.SerieRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService{
    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    SerieRepository serieRepository;

    @Override
    public PostResponseDTO createPost(CreatePostDTO createPostDTO, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario no encontrado"));

        ContentType type = createPostDTO.getContentType();
        String serieName = createPostDTO.getSerieName();
        boolean hasSerieName = serieName != null && !serieName.isBlank();

        Serie postSerie = null;
        if (hasSerieName) {
            postSerie = serieRepository.findByName(serieName).orElse(null);

            // Validar si la serie no existe
            if (postSerie == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serie no encontrada");
            }
        }

        // Validaciones de tipo de contenido según presencia de serie
        if (postSerie != null) {
            if (type == null || type == ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Si hay serie, el tipo de contenido debe ser ANIME, MANGA o SERIE");
            }
        } else {
            if (type != ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Si no hay serie, el tipo de contenido debe ser GENERAL");
            }
        }


        // Título duplicado para el mismo autor
        if (postRepository.existsByTitleAndAuthor(createPostDTO.getTitle(), author)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya posees un Post con ese título");
        }

        // Procesar categorías
        Set<Category> categories = categoryProcess(createPostDTO.getCategories());

        // Crear y guardar el post
        Post newPost = new Post();
        newPost.setTitle(createPostDTO.getTitle());
        newPost.setContent(createPostDTO.getContent());
        newPost.setContentType(type);
        newPost.setStatus(createPostDTO.getStatus() != null ? createPostDTO.getStatus() : PostStatus.DRAFT);
        newPost.setPrivacy(createPostDTO.getPrivacy() != null ? createPostDTO.getPrivacy() : PrivacyLevel.PUBLIC);
        newPost.setSerie(postSerie);
        newPost.setCategories(categories);
        newPost.setAuthor(author);

        if (createPostDTO.getImageUrls() != null){
            newPost.setImageUrls(createPostDTO.getImageUrls());
        }

        author.setReputationPoints(author.getReputationPoints() + 1);
        Post savedPost = postRepository.save(newPost);

        return toDTO(savedPost);
    }


    @Override
    public Page<PostResponseDTO> getAllPost(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(this::toDTO);
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
        post.setViewsCount(post.getViewsCount() + 1);
        Post savedPost = postRepository.save(post);

        return toDTO(savedPost);
    }

    @Override
    public PostResponseDTO toggleLike(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        User author = post.getAuthor();

        if (user.getLikedPosts().contains(post)) {
            // Ya tenía like → lo quitamos
            user.getLikedPosts().remove(post);
            post.setLikesCount(post.getLikesCount() - 1);
            author.setReputationPoints(author.getReputationPoints() - 1);
        } else {
            // No tenía like → lo añadimos
            user.getLikedPosts().add(post);
            post.setLikesCount(post.getLikesCount() + 1);
            author.setReputationPoints(author.getReputationPoints() + 1);
        }

        userRepository.save(user);
        userRepository.save(author);
        postRepository.save(post);

        return toDTO(post);
    }

    @Override
    public void deletePost(Long id, String currentUsername) {
    Post targetPost = postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException (HttpStatus.NOT_FOUND, "Post no encontrado"));

    if (!targetPost.getAuthor().getUsername().equals(currentUsername)){
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes borrar este post");
    }

    postRepository.delete(targetPost);
    }


    private Set<Category> categoryProcess(List<String> categoryNames) {
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría no válida: " + name)))
                .collect(Collectors.toSet());
    }

    private PostResponseDTO toDTO (Post post) {
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

}
