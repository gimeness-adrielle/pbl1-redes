package me.gimenez.dto.ride;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateRideRequest(
        List<String> routes,
        LocalDate date,
        LocalTime departureTime,
        List<Double> prices,
        int seats
) {
}
