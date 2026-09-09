package me.gimenez.requests;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PublishRideRequest(
        List<String> routes,
        LocalDate date,
        LocalTime departureTime,
        List<Double> prices,
        int seats
) {
}
