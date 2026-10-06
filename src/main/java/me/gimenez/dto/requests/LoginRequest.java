package me.gimenez.dto.requests;

public record LoginRequest(
        String email,
        String password
) {
}
