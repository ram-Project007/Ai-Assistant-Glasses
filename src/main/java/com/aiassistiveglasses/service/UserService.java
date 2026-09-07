package com.aiassistiveglasses.service;

import com.aiassistiveglasses.dto.request.ChangePasswordRequest;
import com.aiassistiveglasses.dto.request.UpdateProfileRequest;
import com.aiassistiveglasses.dto.response.UserResponse;
import com.aiassistiveglasses.entity.User;
import com.aiassistiveglasses.exception.ResourceNotFoundException;
import com.aiassistiveglasses.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(User currentUser) {
        return UserResponse.fromEntity(currentUser);
    }

    @Transactional
    public UserResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only fullName and phoneNumber are mutable here - id, role and
        // password are intentionally untouched by this endpoint.
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
