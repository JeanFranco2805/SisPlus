package com.optical.net.sisplus.repository;

import com.optical.net.sisplus.app.infrastructure.entity.User;
import com.optical.net.sisplus.app.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository - Tests de Persistencia")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Debe guardar usuario correctamente")
    void shouldSaveUser() {
        // Given
        User user = User.builder()
                .name("Juan")
                .lastName("Pérez")
                .cc("1234567890")
                .status(true)
                .build();

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Juan");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe encontrar usuario por ID")
    void shouldFindUserById() {
        // Given
        User user = createAndPersistUser("María", "García", "9876543210");

        // When
        Optional<User> found = userRepository.findById(user.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("María");
        assertThat(found.get().getCc()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("Debe retornar Optional vacío si usuario no existe")
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        // When
        Optional<User> found = userRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe listar todos los usuarios")
    void shouldFindAllUsers() {
        // Given
        createAndPersistUser("Carlos", "Rodríguez", "111111");
        createAndPersistUser("Ana", "Martínez", "222222");
        createAndPersistUser("Luis", "López", "333333");

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(3);
        assertThat(users).extracting(User::getName)
                .containsExactlyInAnyOrder("Carlos", "Ana", "Luis");
    }

    @Test
    @DisplayName("Debe actualizar usuario existente")
    void shouldUpdateUser() {
        // Given
        User user = createAndPersistUser("Pedro", "Sánchez", "444444");
        Long userId = user.getId();

        // When
        user.setName("Pedro Actualizado");
        user.setLastName("Sánchez Actualizado");
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear(); // Limpiar cache de primer nivel

        // Then
        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Pedro Actualizado");
        assertThat(updated.getLastName()).isEqualTo("Sánchez Actualizado");
    }

    @Test
    @DisplayName("Debe eliminar usuario por ID")
    void shouldDeleteUserById() {
        // Given
        User user = createAndPersistUser("Laura", "Fernández", "555555");
        Long userId = user.getId();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        Optional<User> found = userRepository.findById(userId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Debe validar unicidad de cédula")
    void shouldEnforceUniqueCc() {
        // Given
        createAndPersistUser("Usuario1", "Apellido1", "1234567890");

        // When & Then
        assertThatThrownBy(() -> {
            User duplicate = User.builder()
                    .name("Usuario2")
                    .lastName("Apellido2")
                    .cc("1234567890") // Cédula duplicada
                    .status(true)
                    .build();
            userRepository.save(duplicate);
            entityManager.flush();
        }).hasMessageContaining("constraint");
    }

    @Test
    @DisplayName("Debe persistir timestamp de creación automáticamente")
    void shouldPersistCreatedAtAutomatically() {
        // Given
        User user = User.builder()
                .name("Temporal")
                .lastName("Usuario")
                .cc("999999")
                .status(true)
                .build();

        // When
        User saved = userRepository.save(user);
        entityManager.flush();

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe permitir usuarios sin asistencias")
    void shouldAllowUserWithoutAttendances() {
        // Given & When
        User user = createAndPersistUser("Sin", "Asistencias", "777777");

        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user.getAttendances()).isNullOrEmpty();
    }

    // Helper method
    private User createAndPersistUser(String name, String lastName, String cc) {
        User user = User.builder()
                .name(name)
                .lastName(lastName)
                .cc(cc)
                .status(true)
                .build();

        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}