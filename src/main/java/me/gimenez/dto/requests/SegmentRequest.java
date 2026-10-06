package me.gimenez.dto.requests;

import java.time.LocalDateTime;

public record SegmentRequest(
        String origin,
        String destination,
        double price,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt
) {
}
