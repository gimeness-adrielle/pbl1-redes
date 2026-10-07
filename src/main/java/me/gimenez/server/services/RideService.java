package me.gimenez.server.services;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.RideRequest;
import me.gimenez.domain.dto.requests.SegmentRequest;
import me.gimenez.server.exceptions.NotFoundException;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.server.exceptions.RideNotCreatedException;
import me.gimenez.server.repository.ReservationRepository;
import me.gimenez.server.repository.RideRepository;
import me.gimenez.domain.dto.requests.ItineraryRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;

    public void create(RideRequest request, User driver){
        List<Segment> segments = new ArrayList<>();

        for (SegmentRequest s: request.segments()){
            if(s.departureAt().isAfter(s.arrivalAt())){
                throw new RideNotCreatedException("A data e hora de chegada não pode ser antes da data de saída.");
            }
            Segment segment = new Segment(
                    UUID.randomUUID(),
                    "E1", // TODO: por enquanto hardcoded.
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

    public List<ItineraryResponse> searchItinerary(ItineraryRequest request){
        List<Segment> segments = rideRepository.searchItinerary(request.date());

        List<ItineraryResponse> itineraries = new ArrayList<>();
        List<Segment> path = new ArrayList<>();

        dfs(path, request.origin(), request.destination(), segments, itineraries);

        return itineraries;
    }

    public void dfs(List<Segment> path, String currentCity, String destination, List<Segment> segments, List<ItineraryResponse> itineraries){
        if (currentCity.equals(destination)) {
            double totalPrice = path.stream().mapToDouble(Segment::getPrice).sum();
            ItineraryResponse itineraryResponse = new ItineraryResponse(totalPrice, new ArrayList<>(path));

            itineraries.add(itineraryResponse);
            return;
        }

        for (Segment segment : segments) {
            if (!segment.getOrigin().equals(currentCity)) { continue; }

            if (path.contains(segment)) { continue; }

            if (!path.isEmpty()) {
                Segment previous = path.getLast();

                if (segment.getDepartureAt().isBefore(previous.getArrivalAt())) {
                    continue;
                }
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

            // Delete all bookings containing the segments to be deleted.
            List<UUID> segmentsToDelete = ride.segments().stream().map(Segment::getId).toList();
            reservationRepository.deleteBySegmentId(segmentsToDelete);

            rideRepository.delete(rideId);
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao deletar a viagem.", e);
        }
    }
}
