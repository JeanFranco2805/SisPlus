package com.optical.net.sisplus.controllers;
import com.optical.net.sisplus.app.infrastructure.controller.api.UserController;
import com.optical.net.sisplus.app.infrastructure.service.UserService;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController - Tests de Integración")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users - Debe retornar lista de usuarios")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAllUsers() throws Exception {
        // Given
        List<UserResponse> users = Arrays.asList(
                createUserResponse(1L, "Juan", "Pérez", "123456"),
                createUserResponse(2L, "María", "García", "789012")
        );
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Juan")))
                .andExpect(jsonPath("$[0].lastName", is("Pérez")))
                .andExpect(jsonPath("$[1].name", is("María")));

        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("POST /api/users - Debe crear usuario exitosamente")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateUserSuccessfully() throws Exception {
        // Given
        UserRequest request = new UserRequest();
        request.setName("Carlos");
        request.setLastName("Rodríguez");
        request.setCc("456789");

        UserResponse response = createUserResponse(3L, "Carlos", "Rodríguez", "456789");
        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Carlos")))
                .andExpect(jsonPath("$.cc", is("456789")));

        verify(userService).createUser(any(UserRequest.class));
    }

    @Test
    @DisplayName("POST /api/users - Debe validar campos requeridos")
    @WithMockUser(roles = "ADMIN")
    void shouldValidateRequiredFields() throws Exception {
        // Given: Request sin nombre
        UserRequest request = new UserRequest();
        request.setLastName("García");
        request.setCc("123456");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("GET /api/users/{id}/payroll - Debe calcular nómina diaria")
    @WithMockUser(roles = "ADMIN")
    void shouldCalculateDailyPayroll() throws Exception {
        // Given
        LocalDate today = LocalDate.of(2026, 1, 25);
        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Juan")
                .lastName("Pérez")
                .cc("123456")
                .regularHours(8.0)
                .regularPay(63672.0)
                .totalPay(63672.0)
                .build();

        when(userService.calculatePayroll(eq(1L), eq(today), any(), any(), eq("daily")))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/users/1/payroll")
                        .param("period", "daily")
                        .param("date", "2026-01-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regularHours", is(8.0)))
                .andExpect(jsonPath("$.regularPay", is(63672.0)))
                .andExpect(jsonPath("$.totalPay", is(63672.0)));
    }

    @Test
    @DisplayName("GET /api/users/{id}/payroll - Debe calcular nómina mensual")
    @WithMockUser(roles = "ADMIN")
    void shouldCalculateMonthlyPayroll() throws Exception {
        // Given
        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Juan")
                .lastName("Pérez")
                .cc("123456")
                .regularHours(176.0)
                .regularPay(1400784.0)
                .totalPay(1400784.0)
                .build();

        when(userService.calculatePayroll(eq(1L), any(), eq(1), eq(2026), eq("monthly")))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/users/1/payroll")
                        .param("period", "monthly")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regularHours", is(176.0)));
    }

    @Test
    @DisplayName("POST /api/users/{id}/entry - Debe registrar entrada")
    @WithMockUser(roles = "ADMIN")
    void shouldRegisterEntry() throws Exception {
        // Given
        doNothing().when(userService).registerEntry(1L);

        // When & Then
        mockMvc.perform(post("/api/users/1/entry").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entry registered successfully")));

        verify(userService).registerEntry(1L);
    }

    @Test
    @DisplayName("POST /api/users/{id}/exit - Debe registrar salida")
    @WithMockUser(roles = "ADMIN")
    void shouldRegisterExit() throws Exception {
        // Given
        doNothing().when(userService).registerExit(1L);

        // When & Then
        mockMvc.perform(post("/api/users/1/exit").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Exit registered successfully")));

        verify(userService).registerExit(1L);
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Debe actualizar usuario")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUser() throws Exception {
        // Given
        UserRequest request = new UserRequest();
        request.setName("Juan Actualizado");
        request.setLastName("Pérez");
        request.setCc("123456");

        UserResponse response = createUserResponse(1L, "Juan Actualizado", "Pérez", "123456");
        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Juan Actualizado")));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Debe eliminar usuario")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteUser() throws Exception {
        // Given
        when(userService.deleteUserById(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User deleted successfully")));

        verify(userService).deleteUserById(1L);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Debe retornar 404 si usuario no existe")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenUserNotFound() throws Exception {
        // Given
        when(userService.deleteUserById(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/users/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Debe denegar acceso sin autenticación")
    void shouldDenyAccessWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // Helper method
    private UserResponse createUserResponse(Long id, String name, String lastName, String cc) {
        return UserResponse.builder()
                .id(id)
                .name(name)
                .lastName(lastName)
                .cc(cc)
                .build();
    }
}