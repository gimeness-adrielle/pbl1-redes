package me.gimenez.dto.requests;

import java.time.LocalDate;

public record ItineraryRequest(
        String origin,
        String destination,
        LocalDate date
) {
}
