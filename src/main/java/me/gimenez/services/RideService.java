package me.gimenez.services;

import me.gimenez.dto.requests.RideRequest;
import me.gimenez.dto.requests.SegmentRequest;
import me.gimenez.exceptions.NotFoundException;
import me.gimenez.exceptions.PersistenceErrorException;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;
import me.gimenez.model.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.dto.requests.ItineraryRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;

    public RideService(RideRepository rideRepository, ReservationRepository reservationRepository) {
        this.rideRepository = rideRepository;
        this.reservationRepository = reservationRepository;
    }

    public void create(RideRequest request, User driver){
        List<Segment> segments = new ArrayList<>();

        for (SegmentRequest s: request.segments()){
            Segment segment = new Segment(
                    UUID.randomUUID(),
                    "E1", // Por enquanto hardcoded.
                    s.origin(),
                    s.destination(),
                    s.price(),
                    request.seats(),
                    s.departureAt(),
                    s.arrivalAt()
            );

            segments.add(segment);
        }

        Ride ride = new Ride(
                UUID.randomUUID(),
                driver.id(),
                request.seats(),
                request.segments().getFirst().departureAt(),
                segments
        );

        try {
            rideRepository.save(ride);
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao salvar a nova viagem no disco.", e);
        }
    }

    public List<Itinerary> searchItinerary(ItineraryRequest request){
        List<Segment> segments = rideRepository.searchItinerary(request.date());

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

    public List<Ride> getRidesByDriver(UUID driverId){
        return rideRepository.getRidesByDriver(driverId);
    }

    public void deleteRide(UUID rideId){
        try{
            Ride ride = rideRepository.findRideById(rideId);

            if (ride == null) {
                throw new NotFoundException("A viagem com o ID " + rideId + " não foi encontrada.");
            }

            // Deleta todas as reservas que tem aqueles trechos da carona como itinerários.
            List<UUID> segmentToDelete = ride.segments().stream().map(Segment::getId).toList();
            reservationRepository.deleteBySegmentId(segmentToDelete);

            rideRepository.delete(rideId);
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao deletar a viagem.", e);
        }
    }
}
