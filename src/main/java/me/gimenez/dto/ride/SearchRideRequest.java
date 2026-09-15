package me.gimenez.dto.ride;

import java.time.LocalDate;

public record SearchRideRequest(
        String origin,
        String destination,
        LocalDate date
) {
}
