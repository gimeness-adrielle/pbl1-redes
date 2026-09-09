package me.gimenez.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter @Setter
public class Ride {
    private UUID id = UUID.randomUUID();
    private UUID driverId;
    private List<String> route;
    private LocalDate date;
    private LocalTime departureTime;
    private List<Segment> segments;

    public Ride(
            UUID driverId,
            List<String> route,
            LocalDate date,
            LocalTime departureTime,
            List<Segment> segments
    ) {
        this.driverId = driverId;
        this.route = route;
        this.date = date;
        this.departureTime = departureTime;
        this.segments = segments;
    }
}
