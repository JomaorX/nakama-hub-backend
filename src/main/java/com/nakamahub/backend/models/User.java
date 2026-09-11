package com.nakamahub.backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    /**
     * Esta relación faltaba. Comment.author es nullable = false y nadie la cascadeaba,
     * así que borrar una cuenta que hubiera comentado en posts de otros violaba la
     * clave foránea. El borrado de cuentas es ahora lógico, pero dejar el grafo
     * completo hace que un purgado real también funcione.
     */
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    /**
     * Solo cascada de borrado, sin orphanRemoval: los tokens se crean y revocan desde
     * RefreshTokenService, nunca manipulando esta colección, y orphanRemoval podría
     * borrar un token recién emitido si la colección llegara a cargarse.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    private String bio;

    private String avatarUrl;

    @Builder.Default
    private int reputationPoints = 0;

    @ManyToMany
    @JoinTable(
            name = "user_followers",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    @Builder.Default
    private Set<User> followers = new HashSet<>();

    @ManyToMany(mappedBy = "followers")
    @Builder.Default
    private Set<User> following = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "user_likes",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id")
    )
    @Builder.Default
    private Set<Post> likedPosts = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProfilePrivacy privacy = ProfilePrivacy.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    /** Momento en que la cuenta se anonimizó. Null mientras la cuenta sigue viva. */
    private LocalDateTime deletedAt;

    /**
     * Identidad por clave primaria.
     *
     * Con @Data, Lombok generaba equals y hashCode sobre todos los campos: User
     * comparaba sus posts, cada Post comparaba su author y se volvía a User, de modo
     * que meter un usuario en un HashSet acababa en StackOverflowError en cuanto las
     * colecciones dejaban de estar vacías. Además forzaba la carga de las relaciones
     * perezosas. Comparar solo por id es lo que recomienda Hibernate.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.getId());
    }

    /**
     * Constante a propósito: el id es null antes de persistir, así que un hashCode
     * basado en él cambiaría al guardar y la entidad se perdería dentro de cualquier
     * colección con tabla hash.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
