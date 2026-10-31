package me.gimenez.domain.dto;

import me.gimenez.domain.models.Segment;

import java.time.LocalDateTime;
import java.util.UUID;

public final class SegmentDTO {

    public record Request(
            String origin,
            String destination,
            double price,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ) {}

    public record Response(
            UUID id,
            String serverId,
            String origin,
            String destination,
            double price,
            int availableSeats,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ) {

        public static SegmentDTO.Response from(Segment segment) {
            return new SegmentDTO.Response(
                    segment.getId(),
                    segment.getServerId(),
                    segment.getOrigin(),
                    segment.getDestination(),
                    segment.getPrice(),
                    segment.getAvailableSeats(),
                    segment.getDepartureAt(),
                    segment.getArrivalAt()
            );
        }
    }
}
