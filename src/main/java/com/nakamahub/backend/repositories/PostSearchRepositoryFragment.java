package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.ContentType;
import com.nakamahub.backend.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Búsqueda de posts con filtros opcionales, separada para no llenar de ramas una consulta JPQL. */
public interface PostSearchRepositoryFragment {

    /**
     * @param viewerId    identificador del visitante, o null si es anónimo
     * @param text        texto a buscar en título y contenido, opcional
     * @param contentType filtro por tipo de contenido, opcional
     * @param serieName   filtro por nombre exacto de serie, opcional
     * @param category    filtro por nombre exacto de categoría, opcional
     */
    Page<Post> search(Long viewerId, String text, ContentType contentType,
                      String serieName, String category, Pageable pageable);
}
