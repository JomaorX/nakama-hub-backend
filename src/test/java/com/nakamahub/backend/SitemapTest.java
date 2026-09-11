package com.nakamahub.backend;

import com.nakamahub.backend.models.PostStatus;
import com.nakamahub.backend.models.PrivacyLevel;
import com.nakamahub.backend.models.Post;
import com.nakamahub.backend.models.User;
import com.nakamahub.backend.support.IntegrationTest;
import com.nakamahub.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Import(TestDataFactory.class)
class SitemapTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User autor;
    private Post publicado;
    private Post borrador;
    private Post privado;

    @BeforeEach
    void setUp() {
        data.clear();
        autor = data.user("brook");
        publicado = data.post(autor, "Publicado", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
        borrador = data.post(autor, "Borrador", PostStatus.DRAFT, PrivacyLevel.PUBLIC);
        privado = data.post(autor, "Privado", PostStatus.PUBLISHED, PrivacyLevel.PRIVATE);
    }

    @Test
    @DisplayName("El mapa del sitio es público y se sirve como XML")
    void elMapaEsPublicoYEsXml() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
    }

    @Test
    @DisplayName("Incluye la portada, los posts públicos y los perfiles")
    void incluyeLoPublico() throws Exception {
        String xml = mockMvc.perform(get("/sitemap.xml")).andReturn().getResponse().getContentAsString();

        assertThat(xml).contains("<loc>https://nakamahub.test/</loc>");
        assertThat(xml).contains("<loc>https://nakamahub.test/post/" + publicado.getId() + "</loc>");
        assertThat(xml).contains("<loc>https://nakamahub.test/u/brook</loc>");
    }

    @Test
    @DisplayName("No delata borradores ni posts privados")
    void noDelataLoOculto() throws Exception {
        String xml = mockMvc.perform(get("/sitemap.xml")).andReturn().getResponse().getContentAsString();

        assertThat(xml).doesNotContain("/post/" + borrador.getId() + "<");
        assertThat(xml).doesNotContain("/post/" + privado.getId() + "<");
    }

    @Test
    @DisplayName("Los perfiles privados quedan fuera del mapa")
    void losPerfilesPrivadosQuedanFuera() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/users/me/privacy")
                .header("Authorization", data.bearer("brook"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"privacy\": \"PRIVATE\"}"))
                .andExpect(status().isOk());

        String xml = mockMvc.perform(get("/sitemap.xml")).andReturn().getResponse().getContentAsString();
        assertThat(xml).doesNotContain("/u/brook<");
    }
}
