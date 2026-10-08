package me.gimenez.domain.dto.requests;

import java.time.LocalDateTime;
import java.util.Set;

public record PeerItineraryRequest (
    String origin,
    String destination,
    LocalDateTime earliestDeparture,
    Set<String> visitedServers
) { }
