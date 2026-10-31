package me.gimenez.domain.dto;

public record LoginRequest(
        String email,
        String password
) {
}
