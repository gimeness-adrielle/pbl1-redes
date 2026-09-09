package me.gimenez.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Getter @Setter
public class Ride {
    @Setter(AccessLevel.NONE)
    private UUID id = UUID.randomUUID();

    private final UUID driverId;
    private final List<String> route;
    private final LocalDate date;
    private final LocalTime departureTime;
    private final List<Segment> segments;
}
