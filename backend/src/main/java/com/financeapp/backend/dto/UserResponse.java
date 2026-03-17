package com.financeapp.backend.dto;

import com.financeapp.backend.domain.Role;

public record UserResponse(Long id, String name, String username, Role role) {
}
