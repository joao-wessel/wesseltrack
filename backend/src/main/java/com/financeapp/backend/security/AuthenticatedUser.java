package com.financeapp.backend.security;

import com.financeapp.backend.domain.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser {

    private final Long id;
    private final String username;
    private final String name;
    private final String role;

    public AuthenticatedUser(Long id, String username, String name, String role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.role = role;
    }

    public static AuthenticatedUser fromJwt(Jwt jwt) {
        return new AuthenticatedUser(
                jwt.getClaim("uid"),
                jwt.getSubject(),
                jwt.getClaim("name"),
                jwt.getClaim("role")
        );
    }

    public static AuthenticatedUser fromUser(AppUser user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getName(), user.getRole().name());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
