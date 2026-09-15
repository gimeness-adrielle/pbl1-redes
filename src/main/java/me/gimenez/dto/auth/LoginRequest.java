package me.gimenez.dto.auth;

public record LoginRequest(
        String username,
        String password
) {
}
