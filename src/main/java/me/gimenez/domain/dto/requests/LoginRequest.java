package me.gimenez.domain.dto.requests;

public record LoginRequest(
        String email,
        String password
) {
}
