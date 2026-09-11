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
class HealthEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("El estado es público, para que el despliegue sepa cuándo la app está lista")
    void elEstadoEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("No expone detalles internos ni los demás endpoints de actuator")
    void noExponeNadaMas() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist());

        // Los demás endpoints no están expuestos, y además la regla general de
        // seguridad exige autenticación para cualquier ruta no declarada.
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }
}
