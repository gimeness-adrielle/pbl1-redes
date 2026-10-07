package me.gimenez.domain.dto.requests;

public record Request(
        String type,
        Object data
) {
}
