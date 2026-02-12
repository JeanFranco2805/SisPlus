package com.optical.net.sisplus.auth;

import com.optical.net.sisplus.app.infrastructure.controller.api.AuthController;
import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import com.optical.net.sisplus.app.infrastructure.service.AuthService;
import com.optical.net.sisplus.app.infrastructure.web.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController - Tests de Autenticación")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login - Debe autenticar con credenciales válidas")
    @WithAnonymousUser
    void shouldLoginWithValidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                "admin123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        Admin admin = Admin.builder()
                .id(1L)
                .username("admin")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authService.findByUsername("admin")).thenReturn(admin);
        doNothing().when(authService).updateLastLogin("admin");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Login exitoso")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.redirectUrl", is("/dashboard")));

        verify(authenticationManager).authenticate(any());
        verify(authService).updateLastLogin("admin");
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe rechazar credenciales inválidas")
    @WithAnonymousUser
    void shouldRejectInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Credenciales incorrectas")));

        verify(authService, never()).updateLastLogin(any());
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe rechazar request sin username")
    @WithAnonymousUser
    void shouldRejectLoginWithoutUsername() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("admin123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout - Debe cerrar sesión exitosamente")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldLogoutSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Sesión cerrada exitosamente")))
                .andExpect(jsonPath("$.redirectUrl", is("/login")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Debe retornar usuario autenticado")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturnCurrentUser() throws Exception {
        // Given
        Admin admin = Admin.builder()
                .id(1L)
                .username("admin")
                .build();

        when(authService.findByUsername("admin")).thenReturn(admin);

        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.username", is("admin")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Debe retornar 401 sin autenticación")
    @WithAnonymousUser
    void shouldReturn401WithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Debe actualizar lastLogin después de login exitoso")
    @WithAnonymousUser
    void shouldUpdateLastLoginAfterSuccessfulLogin() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin", "admin123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        Admin admin = Admin.builder()
                .id(1L)
                .username("admin")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authService.findByUsername("admin")).thenReturn(admin);

        // When
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Then
        verify(authService).updateLastLogin("admin");
    }

    @Test
    @DisplayName("Debe manejar errores de autenticación inesperados")
    @WithAnonymousUser
    void shouldHandleUnexpectedAuthenticationErrors() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Error de base de datos"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}