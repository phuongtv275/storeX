package com.example.storex.controller;

import com.example.storex.exception.GlobalExceptionHandler;
import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.UserRequestDto;
import com.example.storex.model.dto.UserResponseDto;
import com.example.storex.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/users returns 200 OK with user list")
    void shouldGetAllUsers() throws Exception {
        UserResponseDto user = UserResponseDto.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("john_doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/users/1 returns 200 OK with user")
    void shouldGetUserById() throws Exception {
        UserResponseDto user = UserResponseDto.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/users/999 returns 404 Not Found when user not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/users returns 201 Created with created user")
    void shouldCreateUser() throws Exception {
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        when(userService.createUser(any(UserRequestDto.class))).thenReturn(responseDto);

        String jsonPayload = """
                {
                    "username": "john_doe",
                    "email": "john@example.com",
                    "fullName": "John Doe"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/users returns 400 Bad Request when request body has invalid fields")
    void shouldReturn400WhenCreatingUserWithInvalidFields() throws Exception {
        String invalidPayload = """
                {
                    "username": "ab",
                    "email": "not-an-email",
                    "fullName": ""
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/users returns 409 Conflict when username or email already exists")
    void shouldReturn409WhenDuplicateUsernameOrEmail() throws Exception {
        when(userService.createUser(any(UserRequestDto.class)))
                .thenThrow(new DataIntegrityViolationException("Username is already taken: john_doe"));

        String jsonPayload = """
                {
                    "username": "john_doe",
                    "email": "john@example.com",
                    "fullName": "John Doe"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username is already taken: john_doe"));
    }

    @Test
    @DisplayName("PUT /api/users/1 returns 200 OK with updated user")
    void shouldUpdateUser() throws Exception {
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1L)
                .username("john_updated")
                .email("john_updated@example.com")
                .fullName("John Updated")
                .build();

        when(userService.updateUser(eq(1L), any(UserRequestDto.class))).thenReturn(responseDto);

        String updatePayload = """
                {
                    "username": "john_updated",
                    "email": "john_updated@example.com",
                    "fullName": "John Updated"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_updated"))
                .andExpect(jsonPath("$.email").value("john_updated@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    @DisplayName("PUT /api/users/1 returns 409 Conflict when update causes duplicate username/email")
    void shouldReturn409WhenUpdatingUserWithDuplicateUsername() throws Exception {
        when(userService.updateUser(eq(1L), any(UserRequestDto.class)))
                .thenThrow(new DataIntegrityViolationException("Username is already taken: existing_user"));

        String updatePayload = """
                {
                    "username": "existing_user",
                    "email": "john@example.com",
                    "fullName": "John Doe"
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("PUT /api/users/999 returns 404 Not Found when ID does not exist")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(userService.updateUser(eq(999L), any(UserRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        String updatePayload = """
                {
                    "username": "john_doe",
                    "email": "john@example.com",
                    "fullName": "John Doe"
                }
                """;

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/users/1 returns 204 No Content")
    void shouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/users/999 returns 404 Not Found when ID does not exist")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Unhandled exceptions return 500 with generic error message")
    void shouldReturnGenericMessageOnInternalServerError() throws Exception {
        when(userService.getAllUsers()).thenThrow(new RuntimeException("Database secret credentials leaked!"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An internal error occurred. Please contact support."));
    }

    @Test
    @DisplayName("Malformed JSON payload returns 400 Bad Request")
    void shouldReturn400WhenPayloadIsMalformed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed: json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed request payload or invalid parameter type"));
    }

    @Test
    @DisplayName("Invalid path variable type returns 400 Bad Request")
    void shouldReturn400WhenPathVariableTypeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed request payload or invalid parameter type"));
    }

    @Test
    @DisplayName("PUT /api/users/1 returns 400 Bad Request when validation fails")
    void shouldReturn400WhenUpdatingUserWithInvalidFields() throws Exception {
        String invalidPayload = """
                {
                    "username": "",
                    "email": "not-an-email",
                    "fullName": ""
                }
                """;

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Database-level DataIntegrityViolationException with SQL details is sanitized")
    void shouldSanitizeRawSqlMessageInDataIntegrityViolationException() throws Exception {
        when(userService.createUser(any(UserRequestDto.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement [ERROR: duplicate key value violates unique constraint \"users_username_key\"] [insert into users ...]"));

        String payload = """
                {
                    "username": "john_doe",
                    "email": "john@example.com",
                    "fullName": "John Doe"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Database constraint conflict: A user with this username or email already exists."));
    }
}
