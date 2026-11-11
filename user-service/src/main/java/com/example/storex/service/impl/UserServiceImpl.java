package com.example.storex.service.impl;

import com.example.storex.exception.ResourceNotFoundException;
import com.example.storex.model.dto.UserRequestDto;
import com.example.storex.model.dto.UserResponseDto;
import com.example.storex.model.entity.User;
import com.example.storex.repository.UserRepository;
import com.example.storex.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#id")
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponseDto(user);
    }

    @Override
    @CacheEvict(value = "users", key = "'all'")
    public UserResponseDto createUser(UserRequestDto requestDto) {
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new DataIntegrityViolationException("Username is already taken: " + requestDto.getUsername());
        }
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DataIntegrityViolationException("Email is already taken: " + requestDto.getEmail());
        }

        User user = User.builder()
                .username(requestDto.getUsername())
                .email(requestDto.getEmail())
                .fullName(requestDto.getFullName())
                .build();

        User saved = userRepository.save(user);
        return mapToResponseDto(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'all'"),
            @CacheEvict(value = "user", key = "#id")
    })
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!existing.getUsername().equals(requestDto.getUsername())
                && userRepository.existsByUsername(requestDto.getUsername())) {
            throw new DataIntegrityViolationException("Username is already taken: " + requestDto.getUsername());
        }
        if (!existing.getEmail().equals(requestDto.getEmail())
                && userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DataIntegrityViolationException("Email is already taken: " + requestDto.getEmail());
        }

        existing.setUsername(requestDto.getUsername());
        existing.setEmail(requestDto.getEmail());
        existing.setFullName(requestDto.getFullName());

        User updated = userRepository.save(existing);
        return mapToResponseDto(updated);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'all'"),
            @CacheEvict(value = "user", key = "#id")
    })
    public void deleteUser(Long id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(existing);
    }

    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
