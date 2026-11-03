package me.gimenez.server.rides;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.ItineraryDTO;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.RideDTO;
import me.gimenez.domain.dto.SegmentDTO;
import me.gimenez.domain.models.*;
import me.gimenez.server.exceptions.NotFoundException;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.RideNotCreatedException;
import me.gimenez.server.reservations.ReservationRepository;
import me.gimenez.server.rest.PeerClient;
import me.gimenez.server.transactions.TransactionCoordinatorService;
import me.gimenez.util.LockService;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class RideService {
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;

    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;
    private final TransactionCoordinatorService coordinatorService;
    private final LockService lockService;

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
    public void create(RideDTO.Request request, User driver){
        List<Segment> segments = new ArrayList<>();

        for (SegmentDTO.Request s: request.segments()){
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
    public List<ItineraryDTO.Response> searchItinerary(ItineraryDTO.Request request){
        List<Segment> segments = new ArrayList<>(localSegments(request.date()));

        // It should retrieve all segments from all peers servers. So it do a request to get segments from others servers.
        List<Segment> remoteSegments = peerClient.fetchSegments(request.date());
        log.info("Local segments: {}", segments.size());
        log.info("Remote segments: {}", remoteSegments.size());

        segments.addAll(remoteSegments);

        log.info("Total segments for DFS: {}", segments.size());

        List<ItineraryDTO.Response> itineraries = new ArrayList<>();
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
                    List<ItineraryDTO.Response> itineraries
    ){
        if (currentCity.equalsIgnoreCase(destination)) {
            double totalPrice = path.stream().mapToDouble(Segment::getPrice).sum();
            ItineraryDTO.Response itineraryResponse = new ItineraryDTO.Response(totalPrice, new ArrayList<>(path));

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
    public void delete(UUID rideId){
        Ride ride = rideRepository.findById(rideId);
        if (ride == null) { return; }
        List<Segment> segments =  ride.segments();

        // Isso aqui são reservas do meu servidor local que eu preciso cancelar.
        // retorna qualquer reserva que possui qualquer um dos trechos daquela viagem.
        List<Reservation> localReservations =  reservationRepository
                .findReservationBySegments(segments.stream().map(Segment::getId).toList());

        // Isso aqui são reservas dos servidores remotos que eu preciso cancelar.
        // retorna qualquer reserva que possui qualquer um dos trechos daquela viagem.
        List<Reservation> remoteReservations = peerClient
                .fetchReservations(segments.stream().map(Segment::getId).toList());

        List<Reservation> allReservations = new ArrayList<>(localReservations);
        allReservations.addAll(remoteReservations);

        Map<String, List<UUID>> segmentsByServer = allReservations.stream()
                .flatMap(reservation -> reservation.segments().stream())
                        .collect(Collectors.groupingBy(
                                Segment::getServerId,
                                Collectors.mapping(
                                        Segment::getId,
                                        Collectors.toList()
                                )
                        ));

        List<UUID> mySegmentsIds = segmentsByServer.get(peerProperties.selfId());
        // If the booking transaction contains local segments.
        boolean hasLocalSegments = mySegmentsIds != null;

        // Executes only a local delete reservation.
        if (segmentsByServer.size() == 1 && hasLocalSegments) {
            deleteOnlyLocal(ride, mySegmentsIds);
            return;
        }

        boolean success = coordinatorService
                .remoteCreateReservation(segmentsByServer, hasLocalSegments, TransactionType.CANCEL_RESERVATION, null);

        if (!success) { return; }
        try{
            rideRepository.delete(ride.id());
            reservationRepository.deleteBySegmentId(ride.segments().stream().map(Segment::getId).toList());
            rideRepository.saveAll();
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao deletar a viagem.", e);
        }
    }

    private void deleteOnlyLocal(Ride ride, List<UUID> segmentsIds){
        // Saber o que pertence e o que não pertence a viagem a ser deletada.
        // Necessário porque, se houver trechos na reserva que não vão ser deletados junto com a viagem, deve aumentar a quantidade de assentos.
        List<UUID> rideSegments = ride.segments().stream().map(Segment::getId).toList();

        // Obtém todos os trechos que vão precisar retirar a reserva (ou seja, incrementar os assentos)
        // porque pode ser que a reserva tenha trechos de outras viagens.
        segmentsIds.removeAll(rideSegments);

        List<ReentrantLock> locks = lockService.lock(segmentsIds);

        try{
            rideRepository.releaseSeatsInSegments(segmentsIds);
            rideRepository.delete(ride.id());
            reservationRepository.deleteBySegmentId(rideSegments);
            rideRepository.saveAll();
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao deletar a viagem.", e);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }
}
