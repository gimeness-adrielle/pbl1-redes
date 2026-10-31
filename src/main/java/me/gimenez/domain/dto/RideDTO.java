package me.gimenez.domain.dto;

import me.gimenez.domain.models.Ride;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RideDTO {

    public record Response(
            UUID id,
            UUID driverId,
            int seats,
            LocalDateTime departureAt,
            List<SegmentDTO.Response> segments
    ) {
        public static RideDTO.Response from(Ride ride) {
            return new RideDTO.Response(
                    ride.id(),
                    ride.driverId(),
                    ride.seats(),
                    ride.departureAt(),
                    ride.segments().stream().map(SegmentDTO.Response::from).toList()
            );
        }
    }

    public record Request(
            List<SegmentDTO.Request> segments,
            int seats
    ) {}
}
