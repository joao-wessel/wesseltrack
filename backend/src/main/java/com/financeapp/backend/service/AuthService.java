package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.domain.Role;
import com.financeapp.backend.dto.AuthResponse;
import com.financeapp.backend.dto.ChangePasswordRequest;
import com.financeapp.backend.dto.LoginRequest;
import com.financeapp.backend.dto.RegisterRequest;
import com.financeapp.backend.repository.AppUserRepository;
import com.financeapp.backend.security.JwtService;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final CurrentUserService currentUserService;

    public AuthResponse bootstrapAdmin(RegisterRequest request) {
        if (userRepository.count() > 0) {
            throw new IllegalArgumentException("O administrador inicial ja foi configurado.");
        }

        return toAuthResponse(createUser(request, Role.ADMIN));
    }

    public AppUser createUser(RegisterRequest request, Role fallbackRole) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new EntityExistsException("Nome de usuário já cadastrado.");
        }

        Role role = request.role() == null ? fallbackRole : request.role();
        return userRepository.save(AppUser.builder()
                .name(request.name().trim())
                .username(request.username().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .creditCardDueDay(10)
                .createdAt(Instant.now())
                .build());
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase();
        if (loginAttemptService.isBlocked(username)) {
            throw new BadCredentialsException("Usuário temporariamente bloqueado por tentativas excessivas.");
        }

        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> invalidCredentials(username));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials(username);
        }

        loginAttemptService.loginSucceeded(username);
        return toAuthResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        AppUser user = currentUserService.requireCurrentUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("A senha atual está incorreta.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private BadCredentialsException invalidCredentials(String username) {
        loginAttemptService.loginFailed(username);
        return new BadCredentialsException("Credenciais inválidas.");
    }

    public AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                new AuthResponse.UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole())
        );
    }
}
