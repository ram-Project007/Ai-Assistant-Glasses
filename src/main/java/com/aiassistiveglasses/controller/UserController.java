package com.aiassistiveglasses.controller;

import com.aiassistiveglasses.dto.request.ChangePasswordRequest;
import com.aiassistiveglasses.dto.request.UpdateProfileRequest;
import com.aiassistiveglasses.dto.response.ApiResponse;
import com.aiassistiveglasses.dto.response.UserResponse;
import com.aiassistiveglasses.entity.User;
import com.aiassistiveglasses.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Authenticated user's own profile")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", userService.getProfile(currentUser)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's fullName/phoneNumber")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse updated = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
