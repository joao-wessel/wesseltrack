package com.financeapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe a senha atual.")
        String currentPassword,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 120, message = "A nova senha deve ter entre 8 e 120 caracteres.")
        String newPassword,

        @NotBlank(message = "Confirme a nova senha.")
        String confirmPassword
) {
}
