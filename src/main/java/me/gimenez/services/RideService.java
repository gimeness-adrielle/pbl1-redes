package me.gimenez.services;

import me.gimenez.model.Itinerary;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.dto.ride.SearchRideRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RideService {

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;

    public RideService(RideRepository rideRepository, ReservationRepository reservationRepository) {
        this.rideRepository = rideRepository;
        this.reservationRepository = reservationRepository;
    }

    public Ride publish(CreateRideRequest request, User driver){
        List<Segment> segments = new ArrayList<>();

        for (int i = 0; i < request.routes().size() - 1; i++) {
            Segment segment = new Segment(
                    UUID.randomUUID(),
                    request.routes().get(i),
                    request.routes().get(i+1),
                    request.prices().get(i),
                    request.seats()
            );

            segments.add(segment);
        }

        Ride ride = new Ride(driver.id(), request.routes(), request.date(), request.departureTime(), segments);

        try {
            rideRepository.save(ride);
            return ride;
        } catch (IOException e) {
            return null;
        }
    }

    public List<Itinerary> search(SearchRideRequest request){
        List<Ride> rides;
        try {
            rides = rideRepository.findAllRides();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Filtra todas as caronas que estão na data desejada
        List<Ride> ridesOfDate = rides.stream().filter(ride -> ride.getDate().equals(request.date())).toList();

        // Filtra os trechos das caronas que possuem assentos disponíveis
        List<Segment> segments = ridesOfDate.stream().flatMap(ride -> ride.getSegments().stream())
                .filter(segment -> segment.getAvailableSeats() > 0)
                .toList();

        List<Itinerary> itineraries = new ArrayList<>();
        List<Segment> path = new ArrayList<>();

        dfs(path, request.origin(), request.destination(), segments, itineraries);

        return itineraries;
    }

    public void dfs(List<Segment> path, String currentCity, String destination, List<Segment> segments, List<Itinerary> itineraries){
        if (currentCity.equals(destination)) {
            double totalPrice = path.stream().mapToDouble(Segment::getPrice).sum();
            Itinerary itinerary = new Itinerary(totalPrice, new ArrayList<>(path));

            itineraries.add(itinerary);
            return;
        }

        for (Segment segment : segments) {
            if (!segment.getOrigin().equals(currentCity)) {
                continue;
            }

            if (path.contains(segment)) {
                continue;
            }

            path.add(segment);
            dfs(path, segment.getDestination(), destination, segments, itineraries);
            path.removeLast();
        }

    }

    public List<Ride> listRides(){
        try {
            return rideRepository.findAllRides();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteRide(UUID id){
        try{
            Ride ride = rideRepository.findRideById(id);

            if (ride == null) { return false; }

            // Deleta todas as reservas que tem aqueles trechos da carona como itinerários.
            List<UUID> segmentIds = ride.getSegments().stream().map(Segment::getId).toList();
            reservationRepository.deleteBySegmentId(segmentIds);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return rideRepository.delete(id);
    }
}
