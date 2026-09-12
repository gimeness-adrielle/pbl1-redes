package me.gimenez.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Segment {
    private UUID id;
    private String origin;
    private String destination;
    private double price;
    private int availableSeats;

    public  Segment(UUID id, String origin, String destination, double price, int availableSeats) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.price = price;
        this.availableSeats = availableSeats;
    }
}
