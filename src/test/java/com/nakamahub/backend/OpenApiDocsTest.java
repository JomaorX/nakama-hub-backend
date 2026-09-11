package com.nakamahub.backend;

import com.nakamahub.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class OpenApiDocsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("El contrato OpenAPI se genera y describe los endpoints y la autenticación")
    void elContratoOpenApiSeGenera() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Nakama Hub API"))
                .andExpect(jsonPath("$.paths['/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/auth/refresh']").exists())
                .andExpect(jsonPath("$.paths['/api/posts/feed']").exists())
                .andExpect(jsonPath("$.paths['/api/search/posts']").exists())
                .andExpect(jsonPath("$.paths['/api/reports']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
