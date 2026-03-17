package com.financeapp.backend.dto;

import com.financeapp.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(min = 4, max = 80) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,
        @NotBlank @Size(min = 8, max = 120) String password,
        Role role
) {
}
