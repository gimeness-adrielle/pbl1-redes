package me.gimenez.requests;

import me.gimenez.model.Itinerary;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        Itinerary itinerary,
        List<UUID> segmentsIds
) {
}
