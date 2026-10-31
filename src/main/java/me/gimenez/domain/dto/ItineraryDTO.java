package me.gimenez.domain.dto;

import me.gimenez.domain.models.Segment;

import java.time.LocalDate;
import java.util.List;

public final class ItineraryDTO {

    public record Request(
            String origin,
            String destination,
            LocalDate date
    ) {}

    public record Response(
            double totalPrice,
            List<Segment> segments
    ) {}
}
