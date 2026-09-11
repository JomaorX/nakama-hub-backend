package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.post.PostResponseDTO;
import com.nakamahub.backend.dtos.search.UserSearchResultDTO;
import com.nakamahub.backend.dtos.serie.SerieResponseDTO;
import com.nakamahub.backend.models.ContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchService {

    /** @param viewerUsername null en peticiones anónimas. Los resultados respetan la privacidad. */
    Page<PostResponseDTO> searchPosts(String text, ContentType contentType, String serieName,
                                      String category, Pageable pageable, String viewerUsername);

    Page<UserSearchResultDTO> searchUsers(String text, Pageable pageable);

    Page<SerieResponseDTO> searchSeries(String text, Pageable pageable);
}
