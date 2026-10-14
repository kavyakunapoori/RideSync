package com.carpool.ridesharing.service;

import com.carpool.ridesharing.dto.UserProfileDto;
import com.carpool.ridesharing.dto.UserRegistrationDto;
import com.carpool.ridesharing.entity.Role;
import com.carpool.ridesharing.entity.User;
import com.carpool.ridesharing.exception.InvalidOperationException;
import com.carpool.ridesharing.exception.ResourceNotFoundException;
import com.carpool.ridesharing.repository.RoleRepository;
import com.carpool.ridesharing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new InvalidOperationException("An account with this email address already exists.");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new InvalidOperationException("Passwords do not match.");
        }

        User user = new User();
        user.setEmail(dto.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone());
        user.setActive(true);

        String roleName = "ROLE_" + dto.getRole().toUpperCase().trim();
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        user.setRoles(new HashSet<>(List.of(role)));

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User updateProfile(Long userId, UserProfileDto dto) {
        User user = getById(userId);
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone());
        return userRepository.save(user);
    }

    @Transactional
    public boolean toggleUserStatus(Long userId) {
        User user = getById(userId);
        user.setActive(!user.isActive());
        userRepository.save(user);
        return user.isActive();
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }
}
