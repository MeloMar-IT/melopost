package com.melomarit.melopost.controller;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setUuid(userId);
        user.setUsername("testuser");
    }

    @Test
    void getAll_shouldReturnList() {
        when(userService.findAll()).thenReturn(Arrays.asList(user));
        List<User> result = userController.getAll();
        assertEquals(1, result.size());
    }

    @Test
    void getById_shouldReturnUser() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        ResponseEntity<User> result = userController.getById(userId);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(user, result.getBody());
    }

    @Test
    void getById_notFound() {
        when(userService.findById(userId)).thenReturn(Optional.empty());
        ResponseEntity<User> result = userController.getById(userId);
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void create_shouldSaveUser() {
        when(userService.save(any(User.class))).thenReturn(user);
        User result = userController.create(new User());
        assertNotNull(result);
        verify(userService).save(any(User.class));
    }

    @Test
    void update_shouldUpdateExisting() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(userService.save(any(User.class))).thenReturn(user);
        
        User updateData = new User();
        updateData.setUsername("newname");
        
        ResponseEntity<User> result = userController.update(userId, updateData);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("newname", user.getUsername());
    }

    @Test
    void delete_shouldCallService() {
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        ResponseEntity<Void> result = userController.delete(userId);
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(userService).deleteById(userId);
    }
}
