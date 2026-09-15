package me.gimenez.services;

import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Segment;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RideRepository rideRepository;

    public ReservationService(ReservationRepository reservationRepository, RideRepository rideRepository) {
        this.reservationRepository = reservationRepository;
        this.rideRepository = rideRepository;
    }

    public Reservation reserve(Itinerary itinerary, List<UUID> segmentIds, User user){
        List<Segment> currentSegments = new ArrayList<>();

        // Primeiro encontra os trechos verdadeiros salvos no sistema
        try {
            for (UUID segmentId : segmentIds) {
                Segment segment = rideRepository.findSegmentById(segmentId);

                if (segment == null) {
                    return null;
                }

                currentSegments.add(segment);
            }
        } catch (IOException e) {
            return null;
        }

        // Verifica se todos esses trechos ainda tem assentos disponíveis.
        for (Segment segment : currentSegments) {
            if (segment.getAvailableSeats() <= 0) {
                return null;
            }
        }

        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                user.id(),
                itinerary
        );

        try {
            // Salva a reserva e diminui a quantidade de assentos disponíveis do trecho
            reservationRepository.save(reservation);
            for (Segment segment : currentSegments){
                segment.setAvailableSeats(segment.getAvailableSeats() - 1);
            }

            // Salva a alteração nos assentos dos trechos.
            rideRepository.saveAll();
        } catch (IOException e) {
            return null;
        }

        return reservation;
    }

    public List<Reservation> listAllUserReservations(UUID userId){
        return reservationRepository.listAllUserReservations(userId);
    }

    public boolean delete(UUID id){
        Reservation reservation = reservationRepository.getById(id);
        if(reservation == null){
            return false;
        }

        List<Segment> segments = reservation.itinerary().segments();

        try {
            for(Segment segment : segments){
                segment.setAvailableSeats(segment.getAvailableSeats() + 1);
            }
            rideRepository.saveAll();

            return reservationRepository.delete(id);
        } catch (IOException e) {
            return false;
        }
    }

}
