package com.financeapp.backend.service;

import com.financeapp.backend.domain.Role;
import com.financeapp.backend.dto.RegisterRequest;
import com.financeapp.backend.dto.UserCreateRequest;
import com.financeapp.backend.dto.UserUpdateRequest;
import com.financeapp.backend.dto.UserResponse;
import com.financeapp.backend.repository.AppUserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository userRepository;
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole()))
                .toList();
    }

    public UserResponse create(UserCreateRequest request) {
        Role role = request.role() == null ? Role.USER : request.role();
        var user = authService.createUser(new RegisterRequest(
                request.name(),
                request.username(),
                request.password(),
                request.role()
        ), role);
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole());
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        var currentUser = currentUserService.requireCurrentUser();
        var user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        String normalizedUsername = request.username().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, id)) {
            throw new EntityExistsException("Nome de usuário já cadastrado.");
        }

        Role targetRole = request.role() == null ? user.getRole() : request.role();
        if (user.getRole() == Role.ADMIN && targetRole != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Não é possível remover o último administrador.");
        }

        user.setName(request.name().trim());
        user.setUsername(normalizedUsername);
        user.setRole(targetRole);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (currentUser.getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Você não pode remover seu próprio acesso de administrador.");
        }

        user = userRepository.save(user);
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole());
    }

    public void delete(Long id) {
        var currentUser = currentUserService.requireCurrentUser();
        var user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        if (currentUser.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Você não pode excluir seu próprio usuário.");
        }

        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Não é possível excluir o último administrador.");
        }

        userRepository.delete(user);
    }
}
