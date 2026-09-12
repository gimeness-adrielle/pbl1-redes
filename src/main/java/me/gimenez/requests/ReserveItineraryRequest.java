package me.gimenez.requests;

import java.util.List;
import java.util.UUID;

public record ReserveItineraryRequest (
        List<UUID> segmentsIds
) {
}
