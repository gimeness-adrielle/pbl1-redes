package me.gimenez.domain.dto;

import me.gimenez.domain.models.Segment;

import java.util.List;
import java.util.UUID;

public final class ReservationDTO {

    public record Request (
            List<Segment> segments
    ){}

    public record Response(
            UUID id,
            List<SegmentDTO.Response> segments
    ) {}

    public record Data(
            UUID reservationId,
            UUID userId,
            List<Segment> segments
    ) {}
}
