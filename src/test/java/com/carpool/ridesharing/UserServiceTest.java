package com.carpool.ridesharing;

import com.carpool.ridesharing.dto.UserRegistrationDto;
import com.carpool.ridesharing.entity.Role;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.repository.RoleRepository;
import com.carpool.ridesharing.repository.UserRepository;
import com.carpool.ridesharing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should successfully register user and encode password with BCrypt")
    void testSuccessfulRegistration() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setFullName("Test User");
        dto.setEmail("test@carpool.com");
        dto.setPassword("Secret123!");
        dto.setConfirmPassword("Secret123!");
        dto.setRole("PASSENGER");

        when(userRepository.existsByEmail("test@carpool.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("$2a$10$hashedPasswordHere");
        when(roleRepository.findByName("ROLE_PASSENGER")).thenReturn(Optional.of(new Role("ROLE_PASSENGER")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(dto);

        assertNotNull(registered);
        assertEquals("test@carpool.com", registered.getEmail());
        assertEquals("$2a$10$hashedPasswordHere", registered.getPassword());
        assertTrue(registered.hasRole("ROLE_PASSENGER"));
        assertTrue(registered.isActive());
    }

    @Test
    @DisplayName("Should reject registration if email is already taken")
    void testDuplicateEmailRegistration() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("existing@carpool.com");
        dto.setPassword("Secret123!");
        dto.setConfirmPassword("Secret123!");
        dto.setRole("DRIVER");

        when(userRepository.existsByEmail("existing@carpool.com")).thenReturn(true);

        assertThrows(InvalidOperationException.class, () -> {
            userService.registerUser(dto);
        });
    }

    @Test
    @DisplayName("Should toggle user active/deactivated status")
    void testToggleUserStatus() {
        User user = new User("user@carpool.com", "pass", "Name", "123");
        user.setId(5L);
        user.setActive(true);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        boolean statusAfter1 = userService.toggleUserStatus(5L);
        assertFalse(statusAfter1);
        assertFalse(user.isActive());

        boolean statusAfter2 = userService.toggleUserStatus(5L);
        assertTrue(statusAfter2);
        assertTrue(user.isActive());
    }
}
