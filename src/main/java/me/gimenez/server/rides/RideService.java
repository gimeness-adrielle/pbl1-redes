package me.gimenez.server.rides;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.requests.RideRequest;
import me.gimenez.domain.dto.requests.SegmentRequest;
import me.gimenez.server.exceptions.NotFoundException;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.server.exceptions.RideNotCreatedException;
import me.gimenez.server.reservations.ReservationRepository;
import me.gimenez.domain.dto.requests.ItineraryRequest;
import me.gimenez.server.communication.PeerClient;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class RideService {
    private final PeerClient peerclient;
    private final PeerProperties peerProperties;

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;

    /** This function calls the {@link RideRepository} class to filter and retrieve all
     * the rides for the driver specified by the unique identifier in the parameter.
     * @param driverId the unique identifier of the driver wanting his rides.
     * @return the list containing all of that driver's rides.
     */
    public List<Ride> getRidesByDriver(UUID driverId){
        return rideRepository.getRidesByDriver(driverId);
    }

    /**
     * This function is responsible for creating a ride from the request data.
     * @param request these are data submitted by the user, specifying details of the ride to be created.
     * @param driver this is the driver creating this ride. This is useful to for identifying who the ride belongs to.
     */
    public void create(RideRequest request, User driver){
        List<Segment> segments = new ArrayList<>();

        for (SegmentRequest s: request.segments()){
            if(s.departureAt().isAfter(s.arrivalAt())){
                throw new RideNotCreatedException("A data e hora de chegada não pode ser antes da data de saída.");
            }
            Segment segment = new Segment(
                    UUID.randomUUID(),
                    peerProperties.selfId(),
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

    public List<Segment> localSegments(LocalDate date){
        return rideRepository.searchItinerary(date);
    }

    /** Retrieves itineraries based on the data submitted in the request. This function filters for
     * segments with available seats on the date specified in the request. It then executes a DFS
     * algorithm to find itineraries covering all possible segments, including those from other
     * associated servers.
     * @param request this is sent by the passenger to search for itineraries with a specified origin,
     *                destination and date.
     * @return a list containing all the itineraries found, with the required data specified in the request.
     */
    public List<ItineraryResponse> searchItinerary(ItineraryRequest request){
        List<Segment> segments = new ArrayList<>(localSegments(request.date()));

        // It should retrieve all segments from all peers servers. So it do a request to get segments from others servers.
        List<Segment> remoteSegments = peerclient.fetchSegments(request.date());
        log.info("Local segments: {}", segments.size());
        log.info("Remote segments: {}", remoteSegments.size());

        segments.addAll(remoteSegments);

        log.info("Total segments for DFS: {}", segments.size());

        List<ItineraryResponse> itineraries = new ArrayList<>();
        List<Segment> path = new ArrayList<>();

        dfs(
                path,
                request.origin(),
                request.destination(),
                request.date().atStartOfDay(),
                segments,
                itineraries
        );

        return itineraries;
    }

    /** This is a DFS algorithm to find all possible itineraries, taking advantage of the
     * backtracking operation of a DFS.
     * @param path the path to be formed by recursion.
     * @param currentCity the current city to be explored.
     * @param destination the desired destination.
     * @param earliestDeparture the arrival at previous city, to ensures temporal alignment of
     *                          the segments.
     * @param segments segments list of segments with available seats and on the user-specified date.
     * @param itineraries itineraries to be formed.
     */
    public void dfs(List<Segment> path,
                    String currentCity,
                    String destination,
                    LocalDateTime earliestDeparture,
                    List<Segment> segments,
                    List<ItineraryResponse> itineraries
    ){
        if (currentCity.equalsIgnoreCase(destination)) {
            double totalPrice = path.stream().mapToDouble(Segment::getPrice).sum();
            ItineraryResponse itineraryResponse = new ItineraryResponse(totalPrice, new ArrayList<>(path));

            itineraries.add(itineraryResponse);
            return;
        }

        for (Segment segment : segments) {
            if (!segment.getOrigin().equalsIgnoreCase(currentCity)) { continue; }

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
            );
            path.removeLast();
        }
    }

    /** Deletes a ride.
     * Iterates over the ride's segments to delete all reservations that include those ride's segments.
     * Throws {@link NotFoundException} if the unique identifier of the specified ride does not exist.
     * Throws {@link PersistenceErrorException} if any repository operation has failed.
     * @param rideId unique identifier of the ride to be deleted.
     */
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
