package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.search.UserSearchResultDTO;
import com.nakamahub.backend.dtos.serie.SerieResponseDTO;
import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.security.SecurityUtils;
import com.nakamahub.backend.services.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Buscador bajo su propio prefijo para no colisionar con las rutas de recurso,
 * donde /api/users/search competiría con /api/users/{username}.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/posts")
    @ResponseStatus(HttpStatus.OK)
    public Page<PostResponseDTO> searchPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) String serie,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return searchService.searchPosts(q, contentType, serie, category,
                pageable(page, size, "createdAt"), SecurityUtils.currentUsername().orElse(null));
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    public Page<UserSearchResultDTO> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return searchService.searchUsers(q, PageRequest.of(Math.max(0, page), clampSize(size),
                Sort.by("reputationPoints").descending()));
    }

    @GetMapping("/series")
    @ResponseStatus(HttpStatus.OK)
    public Page<SerieResponseDTO> searchSeries(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return searchService.searchSeries(q, PageRequest.of(Math.max(0, page), clampSize(size),
                Sort.by("name").ascending()));
    }

    private static Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(Math.max(0, page), clampSize(size), Sort.by(sortBy).descending());
    }

    private static int clampSize(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}
