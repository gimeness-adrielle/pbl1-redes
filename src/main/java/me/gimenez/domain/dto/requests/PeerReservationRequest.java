package me.gimenez.domain.dto.requests;

import java.util.List;
import java.util.UUID;

public record PeerReservationRequest (
        UUID reservationId,
        UUID passengerId,
        String originServer,
        long logicalTimestamp,
        List<UUID> segmentsIds
){}
