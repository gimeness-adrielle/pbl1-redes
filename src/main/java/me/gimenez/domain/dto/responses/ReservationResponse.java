package me.gimenez.domain.dto.responses;

import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        String status
) {}
