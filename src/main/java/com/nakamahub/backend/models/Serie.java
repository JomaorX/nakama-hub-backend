package com.nakamahub.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "series")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    private String description;

    @OneToMany(mappedBy = "serie")
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    /** Identidad por clave primaria. Ver la explicación en {@link User#equals(Object)}. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Serie serie)) {
            return false;
        }
        return id != null && id.equals(serie.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
