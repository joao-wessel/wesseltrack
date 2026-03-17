package com.financeapp.backend.controller;

import com.financeapp.backend.dto.AuthResponse;
import com.financeapp.backend.dto.LoginRequest;
import com.financeapp.backend.dto.RegisterRequest;
import com.financeapp.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/bootstrap-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse bootstrapAdmin(@Valid @RequestBody RegisterRequest request) {
        return authService.bootstrapAdmin(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
