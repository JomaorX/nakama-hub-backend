package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Serie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SerieRepository extends JpaRepository <Serie, Long> {
    Optional<Serie> findByName (String name);
    boolean existsByName (String name);
}
