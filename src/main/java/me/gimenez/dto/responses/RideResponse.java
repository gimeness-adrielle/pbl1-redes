package me.gimenez.dto.responses;

import me.gimenez.model.Ride;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RideResponse(
        UUID id,
        UUID driverId,
        int seats,
        LocalDateTime departureAt,
        List<SegmentResponse> segments
) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.id(),
                ride.driverId(),
                ride.seats(),
                ride.departureAt(),
                ride.segments().stream().map(SegmentResponse::from).toList()
        );
    }
}
