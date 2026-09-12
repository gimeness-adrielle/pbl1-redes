package me.gimenez.requests;

public record Response(
        String status,
        String message,
        Object data
) {
}
