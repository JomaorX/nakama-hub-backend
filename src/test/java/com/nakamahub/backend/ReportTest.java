package com.nakamahub.backend;

import com.nakamahub.backend.models.*;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.RequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Import(TestDataFactory.class)
class ReportTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestDataFactory data;

    private User denunciante;
    private User infractor;
    private Post postInfractor;

    @BeforeEach
    void setUp() {
        data.clear();
        denunciante = data.user("koby");
        infractor = data.user("spandam");
        data.user("garp", UserRole.ROLE_MODERATOR);
        postInfractor = data.post(infractor, "Spoilers sin avisar", PostStatus.PUBLISHED, PrivacyLevel.PUBLIC);
    }

    @Test
    @DisplayName("Cualquier usuario autenticado puede reportar un post")
    void cualquierUsuarioPuedeReportar() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPOILER_SIN_AVISO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.reportedAuthorUsername").value("spandam"))
                .andExpect(jsonPath("$.reporterUsername").value("koby"));
    }

    @Test
    @DisplayName("El reporte guarda una copia del contenido denunciado")
    void elReporteGuardaCopiaDelContenido() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPOILER_SIN_AVISO"))
                .andExpect(jsonPath("$.reportedExcerpt", containsString("Spoilers sin avisar")));
    }

    @Test
    @DisplayName("La copia sobrevive aunque después se borre el contenido")
    void laCopiaSobreviveAlBorradoDelContenido() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPOILER_SIN_AVISO"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/posts/" + postInfractor.getId() + "/authority")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reports").header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reportedExcerpt", containsString("Spoilers sin avisar")));
    }

    @Test
    @DisplayName("Reportar exige estar autenticado")
    void reportarExigeToken() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("POST", postInfractor.getId(), "SPAM")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("No se puede reportar contenido inexistente")
    void noSePuedeReportarLoQueNoExiste() throws Exception {
        mockMvc.perform(reportar("koby", "POST", 9999L, "SPAM"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("No se puede reportar el contenido propio ni reportarse a uno mismo")
    void noSePuedeReportarLoPropio() throws Exception {
        mockMvc.perform(reportar("spandam", "POST", postInfractor.getId(), "SPAM"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(reportar("koby", "USER", denunciante.getId(), "SPAM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un mismo usuario no puede duplicar un reporte pendiente")
    void noSePuedeDuplicarUnReportePendiente() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPAM"))
                .andExpect(status().isCreated());

        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPAM"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("La cola de moderación solo es visible para el equipo de moderación")
    void laColaEsSoloParaModeracion() throws Exception {
        mockMvc.perform(get("/api/reports").header(HttpHeaders.AUTHORIZATION, data.bearer("koby")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports").header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un moderador cierra el reporte y queda constancia de quién lo revisó")
    void elModeradorCierraElReporte() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPOILER_SIN_AVISO"))
                .andExpect(status().isCreated());

        Long reportId = idDelPrimerReporte();

        mockMvc.perform(put("/api/reports/" + reportId + "/resolve")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "RESUELTO", "moderatorNote": "Post retirado por spoilers"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESUELTO"))
                .andExpect(jsonPath("$.reviewedByUsername").value("garp"))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Un reporte ya revisado no se puede volver a cerrar")
    void unReporteRevisadoNoSeCierraDosVeces() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPAM"));
        Long reportId = idDelPrimerReporte();

        RequestBuilder cierre = put("/api/reports/" + reportId + "/resolve")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("garp"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "DESCARTADO"}
                        """);

        mockMvc.perform(cierre).andExpect(status().isOk());
        mockMvc.perform(put("/api/reports/" + reportId + "/resolve")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DESCARTADO"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un reporte no se puede cerrar dejándolo en pendiente")
    void noSePuedeCerrarComoPendiente() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPAM"));
        Long reportId = idDelPrimerReporte();

        mockMvc.perform(put("/api/reports/" + reportId + "/resolve")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PENDIENTE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Al cerrarlo desaparece de la cola de pendientes")
    void alCerrarloSaleDeLaCola() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "SPAM"));
        Long reportId = idDelPrimerReporte();

        mockMvc.perform(put("/api/reports/" + reportId + "/resolve")
                .header(HttpHeaders.AUTHORIZATION, data.bearer("garp"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "DESCARTADO"}
                        """));

        mockMvc.perform(get("/api/reports").header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/reports").param("status", "DESCARTADO")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Un motivo de reporte inexistente devuelve 400, no 500")
    void motivoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(reportar("koby", "POST", postInfractor.getId(), "ME_CAE_MAL"))
                .andExpect(status().isBadRequest());
    }

    private Long idDelPrimerReporte() throws Exception {
        String cuerpo = mockMvc.perform(get("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, data.bearer("garp")))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.content[0].id")).longValue();
    }

    private RequestBuilder reportar(String username, String targetType, Long targetId, String reason) {
        return post("/api/reports")
                .header(HttpHeaders.AUTHORIZATION, data.bearer(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(targetType, targetId, reason));
    }

    private String cuerpo(String targetType, Long targetId, String reason) {
        return """
                {"targetType": "%s", "targetId": %d, "reason": "%s", "details": "Lo he visto en el feed"}
                """.formatted(targetType, targetId, reason);
    }
}
