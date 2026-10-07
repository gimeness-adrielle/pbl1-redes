package me.gimenez.domain.dto.requests;

import java.util.List;

public record RideRequest(
        List<SegmentRequest> segments,
        int seats
) {
}
