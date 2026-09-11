package me.gimenez.model;

import java.util.UUID;

public record Segment(
        UUID id, String origin, String destination, double price, int availableSeats) {
}
