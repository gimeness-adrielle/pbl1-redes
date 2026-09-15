package me.gimenez.dto.reservation;

import java.util.UUID;

public record DeleteReservationRequest(
        UUID id
) {
}
