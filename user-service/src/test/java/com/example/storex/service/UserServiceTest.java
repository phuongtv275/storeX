package com.example.storex.service;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.UserRequestDto;
import com.example.storex.model.dto.UserResponseDto;
import com.example.storex.model.entity.User;
import com.example.storex.repository.UserRepository;
import com.example.storex.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UserRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        sampleRequest = UserRequestDto.builder()
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();
    }

    @Test
    @DisplayName("getAllUsers returns all user DTOs as an ArrayList")
    void shouldGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).isInstanceOf(ArrayList.class);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("john_doe");
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById returns user DTO when found")
    void shouldGetUserByIdWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponseDto result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException when not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("createUser saves entity and returns response DTO")
    void shouldCreateUser() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDto result = userService.createUser(sampleRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("john_doe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser throws DataIntegrityViolationException when username already exists")
    void shouldThrowWhenCreateUserWithDuplicateUsername() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(sampleRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser throws DataIntegrityViolationException when email already exists")
    void shouldThrowWhenCreateUserWithDuplicateEmail() {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(sampleRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser updates existing entity and returns response DTO")
    void shouldUpdateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserRequestDto updateRequest = UserRequestDto.builder()
                .username("john_doe")
                .email("john_updated@example.com")
                .fullName("John Updated")
                .build();

        when(userRepository.existsByEmail("john_updated@example.com")).thenReturn(false);

        UserResponseDto result = userService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateUser throws ResourceNotFoundException when ID does not exist")
    void shouldThrowOnUpdateWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("updateUser throws DataIntegrityViolationException when new username is taken")
    void shouldThrowOnUpdateWhenDuplicateUsername() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserRequestDto updateRequest = UserRequestDto.builder()
                .username("taken_username")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        when(userRepository.existsByUsername("taken_username")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser throws DataIntegrityViolationException when new email is taken")
    void shouldThrowOnUpdateWhenDuplicateEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserRequestDto updateRequest = UserRequestDto.builder()
                .username("john_doe")
                .email("taken@example.com")
                .fullName("John Doe")
                .build();

        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser deletes entity when found")
    void shouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        doNothing().when(userRepository).delete(sampleUser);

        userService.deleteUser(1L);

        verify(userRepository).delete(sampleUser);
    }

    @Test
    @DisplayName("deleteUser throws ResourceNotFoundException when ID does not exist")
    void shouldThrowOnDeleteWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("Service methods must declare appropriate Spring Cache annotations")
    void shouldDeclareCacheAnnotations() throws NoSuchMethodException {
        var getAllMethod = UserServiceImpl.class.getMethod("getAllUsers");
        Cacheable getAllCacheable = getAllMethod.getAnnotation(Cacheable.class);
        assertThat(getAllCacheable).isNotNull();
        assertThat(getAllCacheable.value()).contains("users");
        assertThat(getAllCacheable.key()).isEqualTo("'all'");

        var getByIdMethod = UserServiceImpl.class.getMethod("getUserById", Long.class);
        Cacheable getByIdCacheable = getByIdMethod.getAnnotation(Cacheable.class);
        assertThat(getByIdCacheable).isNotNull();
        assertThat(getByIdCacheable.value()).contains("user");
        assertThat(getByIdCacheable.key()).isEqualTo("#id");

        var createMethod = UserServiceImpl.class.getMethod("createUser", UserRequestDto.class);
        CacheEvict createEvict = createMethod.getAnnotation(CacheEvict.class);
        assertThat(createEvict).isNotNull();
        assertThat(createEvict.value()).contains("users");
        assertThat(createEvict.key()).isEqualTo("'all'");

        var updateMethod = UserServiceImpl.class.getMethod("updateUser", Long.class, UserRequestDto.class);
        Caching updateCaching = updateMethod.getAnnotation(Caching.class);
        assertThat(updateCaching).isNotNull();
        assertThat(updateCaching.evict()).hasSize(2);
        assertThat(updateCaching.evict())
                .anyMatch(e -> Arrays.asList(e.value()).contains("users") && "'all'".equals(e.key()))
                .anyMatch(e -> Arrays.asList(e.value()).contains("user") && "#id".equals(e.key()));

        var deleteMethod = UserServiceImpl.class.getMethod("deleteUser", Long.class);
        Caching deleteCaching = deleteMethod.getAnnotation(Caching.class);
        assertThat(deleteCaching).isNotNull();
        assertThat(deleteCaching.evict()).hasSize(2);
        assertThat(deleteCaching.evict())
                .anyMatch(e -> Arrays.asList(e.value()).contains("users") && "'all'".equals(e.key()))
                .anyMatch(e -> Arrays.asList(e.value()).contains("user") && "#id".equals(e.key()));
    }
}
