package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.search.UserSearchResultDTO;
import com.nakamahub.backend.dtos.serie.SerieResponseDTO;
import com.nakamahub.backend.models.AccountStatus;
import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.models.Serie;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.SerieRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SerieRepository serieRepository;
    private final PostMapper postMapper;

    public SearchServiceImpl(PostRepository postRepository,
                             UserRepository userRepository,
                             SerieRepository serieRepository,
                             PostMapper postMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.serieRepository = serieRepository;
        this.postMapper = postMapper;
    }

    @Override
    public Page<PostResponseDTO> searchPosts(String text, ContentType contentType, String serieName,
                                             String category, Pageable pageable, String viewerUsername) {
        Long viewerId = viewerUsername == null ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);

        return postRepository.search(viewerId, text, contentType, serieName, category, pageable)
                .map(postMapper::toDTO);
    }

    @Override
    public Page<UserSearchResultDTO> searchUsers(String text, Pageable pageable) {
        return userRepository
                .findByUsernameContainingIgnoreCaseAndStatus(
                        text == null ? "" : text.trim(), AccountStatus.ACTIVE, pageable)
                .map(user -> UserSearchResultDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .bio(user.getBio())
                        .avatarUrl(user.getAvatarUrl())
                        .followersCount(user.getFollowers().size())
                        .reputationPoints(user.getReputationPoints())
                        .build());
    }

    @Override
    public Page<SerieResponseDTO> searchSeries(String text, Pageable pageable) {
        return serieRepository
                .findByNameContainingIgnoreCase(text == null ? "" : text.trim(), pageable)
                .map(SearchServiceImpl::toDTO);
    }

    private static SerieResponseDTO toDTO(Serie serie) {
        return SerieResponseDTO.builder()
                .id(serie.getId())
                .name(serie.getName())
                .description(serie.getDescription())
                .build();
    }
}
