package com.nakamahub.backend.config;

import com.nakamahub.backend.models.Category;
import com.nakamahub.backend.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    @Autowired
    CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> predefined = List.of("Acción", "Comedia", "Seinen", "Shonen", "Romance", "Ecchi", "Aventura");

        for (String name : predefined) {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(Category.builder().name(name).build());
            }
        }
    }
}
