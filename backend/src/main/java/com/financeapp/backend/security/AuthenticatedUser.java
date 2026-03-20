package com.financeapp.backend.security;
import lombok.Getter;
import org.springframework.security.oauth2.jwt.Jwt;

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
}
