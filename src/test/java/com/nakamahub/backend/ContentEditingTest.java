package com.nakamahub.backend;

import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.UserRepository;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(TestDataFactory.class)
class ContentEditingTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    @Autowired
    UserRepository userRepository;

    private User robin;
    private User brook;
    private Post post;

    @BeforeEach
    void setUp() {
        data.clear();
        robin = data.user("robin");
        brook = data.user("brook");
        post = data.post(robin, "Poneglyphs", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
    }

    @Test
    @DisplayName("El autor edita su post y la respuesta trae el contenido nuevo")
    void elAutorEditaSuPost() throws Exception {
        mockMvc.perform(editar(post.getId(), "robin", """
                        {"title": "Poneglyphs y el Siglo Vacío",
                         "content": "Una revisión completa de la teoría original",
                         "contentType": "GENERAL",
                         "categories": ["Debate"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Poneglyphs y el Siglo Vacío"))
                .andExpect(jsonPath("$.content").value("Una revisión completa de la teoría original"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Un post recién creado no sale marcado como editado")
    void unPostRecienCreadoNoEstaEditado() throws Exception {
        mockMvc.perform(get("/api/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edited").value(false));
    }

    @Test
    @DisplayName("Editar sin cambiar el título no choca con la unicidad por autor")
    void editarSinCambiarElTituloFunciona() throws Exception {
        mockMvc.perform(editar(post.getId(), "robin", """
                        {"title": "Poneglyphs",
                         "content": "Solo corrijo una errata del texto original",
                         "contentType": "GENERAL",
                         "categories": ["Debate"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Poneglyphs"));
    }

    @Test
    @DisplayName("No se puede reutilizar el título de otro post propio")
    void noSePuedeDuplicarElTituloDeOtroPostPropio() throws Exception {
        data.post(robin, "Otro tema", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);

        mockMvc.perform(editar(post.getId(), "robin", """
                        {"title": "Otro tema",
                         "content": "Intento pisar el título de mi otro post",
                         "contentType": "GENERAL",
                         "categories": ["Debate"]}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un usuario ajeno no puede editar el post de otro")
    void otroUsuarioNoPuedeEditar() throws Exception {
        mockMvc.perform(editar(post.getId(), "brook", """
                        {"title": "Secuestrado",
                         "content": "Este post ahora dice lo que yo quiera",
                         "contentType": "GENERAL",
                         "categories": ["Debate"]}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Editar un post exige token")
    void editarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(put("/api/posts/" + post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Anónimo", "content": "Sin identificarme siquiera",
                                 "contentType": "GENERAL", "categories": ["Debate"]}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un borrador se publica editándolo")
    void unBorradorSePublicaEditandolo() throws Exception {
        Post borrador = data.post(robin, "En cocina", PostStatus.DRAFT, PrivacyLevel.PUBLIC);

        mockMvc.perform(editar(borrador.getId(), "robin", """
                        {"title": "En cocina",
                         "content": "Ya está listo para publicarse",
                         "contentType": "GENERAL",
                         "categories": ["Debate"],
                         "status": "PUBLISHED"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/posts/" + borrador.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Editar no reparte puntos de reputación")
    void editarNoDaReputacion() throws Exception {
        int reputacionInicial = userRepository.findById(robin.getId()).orElseThrow().getReputationPoints();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(editar(post.getId(), "robin", """
                    {"title": "Poneglyphs",
                     "content": "Reescribo una y otra vez para ver si suben los puntos",
                     "contentType": "GENERAL",
                     "categories": ["Debate"]}
                    """));
        }

        assertThat(userRepository.findById(robin.getId()).orElseThrow().getReputationPoints())
                .isEqualTo(reputacionInicial);
    }

    @Test
    @DisplayName("La edición valida igual que la creación")
    void laEdicionValidaIgualQueLaCreacion() throws Exception {
        mockMvc.perform(editar(post.getId(), "robin", """
                        {"title": "x", "content": "corto", "contentType": "GENERAL", "categories": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.fieldErrors.categories").exists());
    }

    @Test
    @DisplayName("Al editar se mantiene la coherencia entre serie y tipo de contenido")
    void serieYTipoDeContenidoSiguenSiendoCoherentes() throws Exception {
        mockMvc.perform(editar(post.getId(), "robin", """
                        {"title": "Poneglyphs",
                         "content": "Le pongo serie pero dejo el tipo en GENERAL",
                         "contentType": "GENERAL",
                         "serieName": "One Piece",
                         "categories": ["Debate"]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El autor edita su comentario y updatedAt deja de coincidir con createdAt")
    void elAutorEditaSuComentario() throws Exception {
        Comment comentario = data.comment(brook, post, "Creo que te equivocas en esto");

        mockMvc.perform(put("/api/comments/" + comentario.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("brook"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Lo he pensado mejor y llevas razón"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Lo he pensado mejor y llevas razón"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Un usuario ajeno no puede editar el comentario de otro")
    void otroUsuarioNoPuedeEditarElComentario() throws Exception {
        Comment comentario = data.comment(brook, post, "Creo que te equivocas en esto");

        mockMvc.perform(put("/api/comments/" + comentario.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("robin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Retiro lo dicho, el autor tiene razón"}
                                """))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder editar(Long postId, String username, String body) {
        return put("/api/posts/" + postId)
                .header(HttpHeaders.AUTHORIZATION, data.bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
