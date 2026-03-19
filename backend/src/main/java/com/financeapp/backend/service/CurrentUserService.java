package com.financeapp.backend.service;

import com.financeapp.backend.domain.AppUser;
import com.financeapp.backend.repository.AppUserRepository;
import com.financeapp.backend.security.AuthenticatedUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public AppUser requireCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new EntityNotFoundException("Usuário autenticado não encontrado.");
        }

        AuthenticatedUser principal = AuthenticatedUser.fromJwt(jwtAuthenticationToken.getToken());
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }
}