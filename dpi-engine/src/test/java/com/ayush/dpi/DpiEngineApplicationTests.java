package com.ayush.dpi;

import com.ayush.dpi.api.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the DPI Engine application.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DpiEngineApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthController healthController;

    @Test
    @DisplayName("Spring application context loads successfully")
    void contextLoads() {
        assertThat(healthController).isNotNull();
    }

    @Test
    @DisplayName("GET /api/health returns 200 with structured JSON")
    void healthEndpointReturnsStructuredResponse() throws Exception {
        mockMvc.perform(get("/api/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
