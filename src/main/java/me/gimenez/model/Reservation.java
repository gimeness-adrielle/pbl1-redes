package me.gimenez.model;

import java.util.List;
import java.util.UUID;

public record Reservation(
        UUID id,
        UUID passengerId,
        List<Segment> segments,
        double totalPrice
) {
    public Reservation(UUID id, UUID passengerId, List<Segment> segments) {
        this(id, passengerId, segments, segments.stream().mapToDouble(Segment::getPrice).sum());
    }
}
