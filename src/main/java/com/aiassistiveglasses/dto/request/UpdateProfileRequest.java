package com.aiassistiveglasses.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fields a user is allowed to self-update. Deliberately excludes id, role,
 * and password (password changes go through ChangePasswordRequest).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+\\-\\s]{7,15}$", message = "Phone number must be valid")
    private String phoneNumber;
}
