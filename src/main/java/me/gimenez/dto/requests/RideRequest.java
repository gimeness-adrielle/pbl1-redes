package me.gimenez.dto.requests;

import java.util.List;

public record RideRequest(
        List<SegmentRequest> segments,
        int seats
) {
}
