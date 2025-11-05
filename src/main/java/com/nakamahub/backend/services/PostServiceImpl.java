package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.CreatePostDTO;
import com.nakamahub.backend.dtos.PostResponseDTO;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.SerieRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        User author = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usuario no encontrado"));
        boolean hasSerie = createPostDTO.getSerieName() != null && !createPostDTO.getSerieName().isBlank();
        System.out.println("DTO RECIBIDO: " + createPostDTO);

        ContentType type = createPostDTO.getContentType();
        Serie postSerie = null;

        if (createPostDTO.getSerieName() != null && !createPostDTO.getSerieName().isBlank()) {
            postSerie = serieRepository.findByName(createPostDTO.getSerieName())
                    .orElse(null);
        }


        if (postSerie != null) {
            if (type == null || type == ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Si hay serie, el tipo de contenido debe ser ANIME, MANGA o SERIE");
            }
        } else {
            if (type != ContentType.GENERAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Si no hay serie, el tipo de contenido debe ser GENERAL");
            }
        }


        if (createPostDTO.getSerieName() != null && !createPostDTO.getSerieName().isBlank() && postSerie == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Serie no encontrada");
        }



        if (postRepository.existsByTitleAndAuthor(createPostDTO.getTitle(), author)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya posees un Post con ese título");
        }

        Set<Category> categories = categoryProcess(createPostDTO.getCategories());

        Post newPost = new Post();

        newPost.setTitle(createPostDTO.getTitle());
        newPost.setContent(createPostDTO.getContent());
        newPost.setContentType(createPostDTO.getContentType());
        newPost.setSerie(postSerie);
        newPost.setCategories(categories);
        newPost.setAuthor(author);

        Post savedPost = postRepository.save(newPost);

        return PostResponseDTO.builder()
                .id(savedPost.getId())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .categories(createPostDTO.getCategories())
                .authorUsername(author.getUsername())
                .serieName(savedPost.getSerie().getName())
                .contentType(savedPost.getContentType())
                .build();
    }

    @Override
    public List<PostResponseDTO> getAllPost() {
        return postRepository.findAll().stream()
                .map(post -> PostResponseDTO.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .categories(post.getCategories().stream().map(Category::getName).toList())
                        .authorUsername(post.getAuthor().getUsername())
                        .serieName(post.getSerie() != null ? post.getSerie().getName() : null)
                        .contentType(post.getContentType())
                        .build())
                .toList();
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));

        return PostResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categories(post.getCategories().stream().map(Category::getName).toList())
                .authorUsername(post.getAuthor().getUsername())
                .serieName(post.getSerie() != null ? post.getSerie().getName() : null)
                .contentType(post.getContentType())
                .build();
    }

    private Set<Category> categoryProcess(List<String> categoryNames) {
        return categoryNames.stream()
                .map(name -> categoryRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría no válida: " + name)))
                .collect(Collectors.toSet());
    }

}
