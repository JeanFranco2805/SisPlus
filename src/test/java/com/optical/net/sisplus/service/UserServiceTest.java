package com.optical.net.sisplus.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.service.UserService;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Tests Unitarios")
class UserServiceTest {

    @Mock
    private PortAdapter portAdapter;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<UserDomain> userDomainCaptor;

    private UserDomain sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = UserDomain.builder()
                .id(1L)
                .name("Juan")
                .lastName("Pérez")
                .cc("1234567890")
                .build();
    }

    @Test
    @DisplayName("Debe obtener todos los usuarios")
    void shouldGetAllUsers() {
        // Given
        List<UserDomain> users = Arrays.asList(
                UserDomain.builder().id(1L).name("Juan").lastName("Pérez").cc("111").build(),
                UserDomain.builder().id(2L).name("María").lastName("García").cc("222").build()
        );
        when(portAdapter.getAllUsers()).thenReturn(users);

        // When
        List<UserResponse> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Juan");
        assertThat(result.get(1).getName()).isEqualTo("María");
        verify(portAdapter).getAllUsers();
    }

    @Test
    @DisplayName("Debe crear usuario correctamente")
    void shouldCreateUser() {
        // Given
        UserRequest request = new UserRequest();
        request.setName("Carlos");
        request.setLastName("Rodríguez");
        request.setCc("987654321");

        UserDomain savedUser = UserDomain.builder()
                .id(3L)
                .name("Carlos")
                .lastName("Rodríguez")
                .cc("987654321")
                .build();

        when(portAdapter.saveUser(any(UserDomain.class))).thenReturn(savedUser);

        // When
        UserResponse result = userService.createUser(request);

        // Then
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("Carlos");
        assertThat(result.getCc()).isEqualTo("987654321");

        verify(portAdapter).saveUser(userDomainCaptor.capture());
        UserDomain captured = userDomainCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("Carlos");
    }

    @Test
    @DisplayName("Debe actualizar usuario existente")
    void shouldUpdateUser() {
        // Given
        Long userId = 1L;
        UserRequest request = new UserRequest();
        request.setName("Juan Actualizado");
        request.setLastName("Pérez Actualizado");
        request.setCc("1234567890");

        UserDomain updatedUser = UserDomain.builder()
                .id(userId)
                .name("Juan Actualizado")
                .lastName("Pérez Actualizado")
                .cc("1234567890")
                .build();

        when(portAdapter.saveUser(any(UserDomain.class))).thenReturn(updatedUser);

        // When
        UserResponse result = userService.updateUser(userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Juan Actualizado");
        verify(portAdapter).saveUser(any(UserDomain.class));
    }

    @Test
    @DisplayName("Debe eliminar usuario por ID")
    void shouldDeleteUserById() {
        // Given
        Long userId = 1L;
        doNothing().when(portAdapter).deleteUser(userId);

        // When
        boolean result = userService.deleteUserById(userId);

        // Then
        assertThat(result).isTrue();
        verify(portAdapter).deleteUser(userId);
    }

    @Test
    @DisplayName("Debe calcular nómina diaria")
    void shouldCalculateDailyPayroll() {
        // Given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 1, 25);
        when(portAdapter.findUserById(userId)).thenReturn(sampleUser);

        // When
        UserResponse result = userService.calculatePayroll(userId, date, null, null, "daily");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(portAdapter).findUserById(userId);
    }

    @Test
    @DisplayName("Debe calcular nómina mensual")
    void shouldCalculateMonthlyPayroll() {
        // Given
        Long userId = 1L;
        int month = 1;
        int year = 2026;
        when(portAdapter.findUserById(userId)).thenReturn(sampleUser);

        // When
        UserResponse result = userService.calculatePayroll(userId, null, month, year, "monthly");

        // Then
        assertThat(result).isNotNull();
        verify(portAdapter).findUserById(userId);
    }

    @Test
    @DisplayName("Debe usar fecha actual por defecto en cálculo diario")
    void shouldUseCurrentDateByDefault() {
        // Given
        Long userId = 1L;
        when(portAdapter.findUserById(userId)).thenReturn(sampleUser);

        // When
        UserResponse result = userService.calculatePayroll(userId, null, null, null, "daily");

        // Then
        assertThat(result).isNotNull();
        verify(portAdapter).findUserById(userId);
    }

    @Test
    @DisplayName("Debe registrar entrada de empleado")
    void shouldRegisterEntry() {
        // Given
        Long userId = 1L;
        doNothing().when(portAdapter).registerAttendance(userId);

        // When
        userService.registerEntry(userId);

        // Then
        verify(portAdapter).registerAttendance(userId);
    }

    @Test
    @DisplayName("Debe registrar salida de empleado")
    void shouldRegisterExit() {
        // Given
        Long userId = 1L;
        doNothing().when(portAdapter).registerDeparture(userId);

        // When
        userService.registerExit(userId);

        // Then
        verify(portAdapter).registerDeparture(userId);
    }

    @Test
    @DisplayName("Debe propagar excepciones del adaptador")
    void shouldPropagateAdapterExceptions() {
        // Given
        Long userId = 999L;
        when(portAdapter.findUserById(userId))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // When & Then
        assertThatThrownBy(() ->
                userService.calculatePayroll(userId, LocalDate.now(), null, null, "daily")
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}