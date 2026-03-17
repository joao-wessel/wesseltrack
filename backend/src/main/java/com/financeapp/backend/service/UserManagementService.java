package com.financeapp.backend.service;

import com.financeapp.backend.domain.Role;
import com.financeapp.backend.dto.RegisterRequest;
import com.financeapp.backend.dto.UserResponse;
import com.financeapp.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final AppUserRepository userRepository;
    private final AuthService authService;

    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole()))
                .toList();
    }

    public UserResponse create(RegisterRequest request) {
        Role role = request.role() == null ? Role.USER : request.role();
        var user = authService.createUser(request, role);
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole());
    }
}
