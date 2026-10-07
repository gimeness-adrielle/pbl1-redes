package me.gimenez.domain.dto.responses;

import me.gimenez.domain.models.Segment;

import java.util.List;

public record ItineraryResponse(double totalPrice, List<Segment> segments) {
}
