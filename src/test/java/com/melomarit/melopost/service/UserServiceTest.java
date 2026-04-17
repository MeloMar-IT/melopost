package com.melomarit.melopost.service;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setUuid(userId);
        user.setUsername("testuser");
        user.setPassword("password");
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));
        List<User> result = userService.findAll();
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Optional<User> result = userService.findById(userId);
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void save_shouldEncodePassword_whenPasswordIsPlaintext() {
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User savedUser = userService.save(user);

        assertEquals("encodedPassword", savedUser.getPassword());
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(user);
    }

    @Test
    void save_shouldNotEncodePassword_whenPasswordIsAlreadyEncoded() {
        user.setPassword("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User savedUser = userService.save(user);

        assertEquals("$2a$10$encoded", savedUser.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deleteById_shouldCallRepository() {
        userService.deleteById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void findByUsername_shouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        Optional<User> result = userService.findByUsername("testuser");
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }
}
