package me.gimenez.server.services;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
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
import me.gimenez.server.rest.PeerClient;
import me.gimenez.util.ServerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
public class RideService {
    private final PeerClient peerclient;
    private final ServerProperties serverProperties;

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;

    public List<Ride> getRidesByDriver(UUID driverId){
        return rideRepository.getRidesByDriver(driverId);
    }

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
        Set<String> visitedServers = new HashSet<>();
        visitedServers.add(serverProperties.id());

        dfs(
                path,
                request.origin(),
                request.destination(),
                request.date().atStartOfDay(),
                segments,
                itineraries
//                visitedServers
        );

        return itineraries;
    }

    public List<ItineraryResponse> searchItinerary(PeerItineraryRequest request){
        if (request.visitedServers().contains(serverProperties.id())){
            return null;
        }
        request.visitedServers().add(serverProperties.id());

        List<Segment> segments = rideRepository.searchItinerary(request.earliestDeparture().toLocalDate());

        List<ItineraryResponse> itineraries = new ArrayList<>();
        List<Segment> path = new ArrayList<>();

        dfs(
                path,
                request.origin(),
                request.destination(),
                request.earliestDeparture(),
                segments,
                itineraries
//                request.visitedServers()
        );

        return itineraries;
    }

    public void dfs(List<Segment> path,
                    String currentCity,
                    String destination,
                    LocalDateTime earliestDeparture,
                    List<Segment> segments,
                    List<ItineraryResponse> itineraries
//                    Set<String> visitedServers
    ){
        if (currentCity.equals(destination)) {
            double totalPrice = path.stream().mapToDouble(Segment::getPrice).sum();
            ItineraryResponse itineraryResponse = new ItineraryResponse(totalPrice, new ArrayList<>(path));

            itineraries.add(itineraryResponse);
            return;
        }

        for (Segment segment : segments) {
            if (!segment.getOrigin().equals(currentCity)) { continue; }

            if (path.contains(segment)) { continue; }

            if (segment.getDepartureAt().isBefore(earliestDeparture)) { continue; }

            path.add(segment);
            dfs(
                    path,
                    segment.getDestination(),
                    destination,
                    segment.getArrivalAt(),
                    segments,
                    itineraries
//                    visitedServers
            );
            path.removeLast();
        }
    }

    public void deleteRide(UUID rideId){
        try{
            Ride ride = rideRepository.delete(rideId);

            // Delete all bookings containing the segments to be deleted.
            List<UUID> segmentsToDelete = ride.segments().stream().map(Segment::getId).toList();
            reservationRepository.deleteBySegmentId(segmentsToDelete);

            rideRepository.saveAll();
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao deletar a viagem.", e);
        } catch (NullPointerException e) {
            throw new NotFoundException("A viagem com o ID " + rideId + " não foi encontrada.");
        }
    }
}
