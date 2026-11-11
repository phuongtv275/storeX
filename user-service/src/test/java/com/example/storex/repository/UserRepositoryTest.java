package com.example.storex.repository;

import com.example.storex.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and retrieve a user by ID")
    void shouldSaveAndRetrieveUser() {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        assertThat(found.get().getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should find user by username and by email")
    void shouldFindByUsernameAndEmail() {
        User user = User.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .fullName("Jane Doe")
                .build();

        userRepository.save(user);

        Optional<User> byUsername = userRepository.findByUsername("jane_doe");
        assertThat(byUsername).isPresent();
        assertThat(byUsername.get().getEmail()).isEqualTo("jane@example.com");

        Optional<User> byEmail = userRepository.findByEmail("jane@example.com");
        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().getUsername()).isEqualTo("jane_doe");

        assertThat(userRepository.existsByUsername("jane_doe")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();

        assertThat(userRepository.existsByEmail("jane@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser() {
        User user = User.builder()
                .username("to_delete")
                .email("delete@example.com")
                .fullName("To Delete")
                .build();

        User saved = userRepository.save(user);
        Long id = saved.getId();

        userRepository.delete(saved);

        Optional<User> found = userRepository.findById(id);
        assertThat(found).isEmpty();
    }
}
