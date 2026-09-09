package me.gimenez.requests.auth;

public record LoginRequest(
        String username,
        String password
) {
}
