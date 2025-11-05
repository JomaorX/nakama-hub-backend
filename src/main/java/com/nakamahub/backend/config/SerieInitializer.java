package com.nakamahub.backend.config;

import com.nakamahub.backend.models.Serie;
import com.nakamahub.backend.repositories.CategoryRepository;
import com.nakamahub.backend.repositories.SerieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SerieInitializer implements CommandLineRunner {
    @Autowired
    SerieRepository serieRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> series = List.of("One Piece", "Naruto", "Bleach", "Jujutsu Kaisen", "Attack on Titan");

        for (String name : series) {
            if (!serieRepository.existsByName(name)) {
                serieRepository.save(Serie.builder().name(name).build());
            }
        }
    }
}
