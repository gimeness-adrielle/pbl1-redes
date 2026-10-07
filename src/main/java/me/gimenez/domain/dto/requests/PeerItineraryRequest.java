package me.gimenez.domain.dto.requests;

import java.time.LocalDateTime;
import java.util.List;

public record PeerItineraryRequest (
    String origin,
    String destination,
    LocalDateTime earliestDeparture,
    List<String> visitedServers
) { }
