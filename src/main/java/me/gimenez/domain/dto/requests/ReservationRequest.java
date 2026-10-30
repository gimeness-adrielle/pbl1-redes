package me.gimenez.domain.dto.requests;

import me.gimenez.domain.models.Segment;

import java.util.List;

public record ReservationRequest (
        List<Segment> segments
){
}
