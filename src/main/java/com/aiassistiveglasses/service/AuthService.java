package com.aiassistiveglasses.service;

import com.aiassistiveglasses.dto.request.LoginRequest;
import com.aiassistiveglasses.dto.request.RegisterRequest;
import com.aiassistiveglasses.dto.response.AuthResponse;
import com.aiassistiveglasses.dto.response.UserResponse;
import com.aiassistiveglasses.entity.Role;
import com.aiassistiveglasses.entity.User;
import com.aiassistiveglasses.exception.DuplicateResourceException;
import com.aiassistiveglasses.repository.UserRepository;
import com.aiassistiveglasses.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    public AuthResponse login(LoginRequest request) {
        // Delegates to Spring Security's AuthenticationManager, which uses
        // CustomUserDetailsService + BCryptPasswordEncoder under the hood.
        // Throws BadCredentialsException / DisabledException on failure,
        // both handled centrally by GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user vanished unexpectedly"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
