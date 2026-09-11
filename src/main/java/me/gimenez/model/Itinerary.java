package me.gimenez.model;

import java.util.List;

public record Itinerary (double totalPrice, List<Segment> segments) {
}
