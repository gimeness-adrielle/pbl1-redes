package me.gimenez.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record Ride (
    UUID id,
    UUID driverId,
    int seats,
    LocalDateTime departureAt,
    List<Segment> segments
){}
