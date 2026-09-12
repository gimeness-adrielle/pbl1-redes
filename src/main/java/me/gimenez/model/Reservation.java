package me.gimenez.model;

import java.util.UUID;

public record Reservation(
        UUID id,
        UUID passengerId,
        Itinerary itinerary
) {
}
