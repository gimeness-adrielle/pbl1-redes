package me.gimenez.requests;

import java.time.LocalDate;

public record SearchRideRequest(
        String origin,
        String destination,
        LocalDate date
) {
}
