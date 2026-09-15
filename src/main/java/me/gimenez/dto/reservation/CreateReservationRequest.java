package me.gimenez.dto.reservation;

import me.gimenez.model.Itinerary;

import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(
        Itinerary itinerary,
        List<UUID> segmentsIds
) {
}
