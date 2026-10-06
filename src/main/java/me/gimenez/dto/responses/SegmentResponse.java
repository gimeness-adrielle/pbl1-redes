package me.gimenez.dto.responses;

import me.gimenez.model.Segment;

import java.time.LocalDateTime;
import java.util.UUID;

public record SegmentResponse(
        UUID id,
        String origin,
        String destination,
        double price,
        int availableSeats,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt
) {

    public static SegmentResponse from(Segment segment) {
        return new SegmentResponse(
                segment.getId(),
                segment.getOrigin(),
                segment.getDestination(),
                segment.getPrice(),
                segment.getAvailableSeats(),
                segment.getDepartureAt(),
                segment.getArrivalAt()
        );
    }
}
