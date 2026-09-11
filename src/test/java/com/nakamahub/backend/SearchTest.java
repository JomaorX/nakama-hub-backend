package com.nakamahub.backend;

import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class SearchTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User autor;

    @BeforeEach
    void setUp() {
        data.clear();
        autor = data.user("kaido");
        data.post(autor, "Guía de Wano", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.post(autor, "Ranking de openings", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        data.post(autor, "Borrador sobre Wano", PostStatus.DRAFT, PrivacyLevel.PUBLIC);
        data.post(autor, "Secreto de Wano", PostStatus.PUBLISHED, PrivacyLevel.PRIVATE);
    }

    @Test
    @DisplayName("La búsqueda por texto encuentra por título sin distinguir mayúsculas")
    void buscaPorTituloSinDistinguirMayusculas() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("q", "wano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Guía de Wano"));
    }

    @Test
    @DisplayName("La búsqueda también mira dentro del contenido")
    void buscaDentroDelContenido() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("q", "Ranking de openings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Un anónimo no encuentra borradores ni posts privados")
    void elAnonimoNoEncuentraLoOculto() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("q", "Wano"))
                .andExpect(jsonPath("$.content[*].title",
                        not(hasItems("Borrador sobre Wano", "Secreto de Wano"))));
    }

    @Test
    @DisplayName("El autor sí encuentra sus propios borradores y privados")
    void elAutorEncuentraLoSuyo() throws Exception {
        mockMvc.perform(get("/api/search/posts")
                        .param("q", "Wano")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("kaido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @DisplayName("Sin texto la búsqueda devuelve todo lo visible, paginado")
    void sinTextoDevuelveTodoLoVisible() throws Exception {
        mockMvc.perform(get("/api/search/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Se puede filtrar por categoría")
    void filtraPorCategoria() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("category", "General"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/search/posts").param("category", "Cosplay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Se puede filtrar por tipo de contenido")
    void filtraPorTipoDeContenido() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("contentType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(get("/api/search/posts").param("contentType", "MANGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Un tipo de contenido inexistente devuelve 400, no 500")
    void tipoDeContenidoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/search/posts").param("contentType", "PELICULA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La búsqueda de usuarios no expone el email")
    void buscaUsuariosSinExponerElEmail() throws Exception {
        mockMvc.perform(get("/api/search/users").param("q", "kai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("kaido"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    @Test
    @DisplayName("La búsqueda de usuarios no lista cuentas borradas")
    void noListaCuentasBorradas() throws Exception {
        data.user("borrable");

        mockMvc.perform(get("/api/search/users").param("q", "borrable"))
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, data.bearer("borrable")));

        mockMvc.perform(get("/api/search/users").param("q", "borrable"))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("La búsqueda de series encuentra las precargadas")
    void buscaSeries() throws Exception {
        mockMvc.perform(get("/api/search/series").param("q", "piece"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("One Piece")));
    }
}
