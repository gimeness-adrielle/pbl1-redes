package me.gimenez.services;

import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;

import java.io.IOException;
import java.util.UUID;

public class ReservationService {
    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    public boolean reserve(Itinerary itinerary, User user){
        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                user.id(),
                itinerary
        );

        try {
            repository.save(reservation);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
