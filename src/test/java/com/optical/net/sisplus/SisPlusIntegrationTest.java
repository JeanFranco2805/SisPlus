package com.optical.net.sisplus;
import com.optical.net.sisplus.app.infrastructure.web.LoginRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Flujo Completo")
class SisPlusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flujo E2E: Login → Crear Empleado → Registrar Asistencia → Calcular Nómina")
    void shouldCompleteFullWorkflow() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        UserRequest userRequest = new UserRequest();
        userRequest.setName("Juan");
        userRequest.setLastName("Pérez");
        userRequest.setCc("1234567890");

        MvcResult createUserResult = mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Juan")))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseBody = createUserResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(post("/api/users/" + userId + "/entry")
                        .with(csrf()))
                .andExpect(status().isOk());

        Thread.sleep(100);

        mockMvc.perform(post("/api/users/" + userId + "/exit")
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + userId + "/payroll")
                        .param("period", "daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.intValue())))
                .andExpect(jsonPath("$.name", is("Juan")))
                .andExpect(jsonPath("$.totalPay").exists());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.id == " + userId + ")].name", hasItem("Juan")));

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Debe validar seguridad en endpoints protegidos")
    void shouldEnforceSecurity() throws Exception {
        // Sin autenticación debe retornar 401/403
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"lastName\":\"User\",\"cc\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debe manejar errores de validación correctamente")
    void shouldHandleValidationErrors() throws Exception {
        // Login primero
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        UserRequest invalidRequest = new UserRequest();
        invalidRequest.setName(""); // Nombre vacío

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe calcular correctamente nómina mensual con múltiples asistencias")
    void shouldCalculateMonthlyPayrollWithMultipleAttendances() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        UserRequest userRequest = new UserRequest();
        userRequest.setName("María");
        userRequest.setLastName("García");
        userRequest.setCc("9876543210");

        MvcResult result = mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/users/" + userId + "/entry").with(csrf()))
                    .andExpect(status().isOk());
            Thread.sleep(50);
            mockMvc.perform(post("/api/users/" + userId + "/exit").with(csrf()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/users/" + userId + "/payroll")
                        .param("period", "monthly")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPay").isNumber())
                .andExpect(jsonPath("$.regularHours").isNumber());
    }
}