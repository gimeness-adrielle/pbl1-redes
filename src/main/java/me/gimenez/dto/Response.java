package me.gimenez.dto;

public record Response(
        String status,
        String message,
        Object data
) {
}
