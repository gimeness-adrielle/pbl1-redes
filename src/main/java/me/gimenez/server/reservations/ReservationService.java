package me.gimenez.server.reservations;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.models.*;
import me.gimenez.server.exceptions.NotFoundException;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.server.rides.RideRepository;
import me.gimenez.server.transactions.TransactionCoordinatorService;
import me.gimenez.server.transactions.TransactionService;
import me.gimenez.util.LockService;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReservationService {
    private final PeerProperties peerProperties;
    private final ReservationRepository reservationRepository;
    private final RideRepository rideRepository;
    private final TransactionCoordinatorService coordinatorService;
    private final LockService lockService;
    private final TransactionService transactionService;

    public Reservation createReservation(List<Segment> segments, User user) {
        Map<String, List<UUID>> segmentsByServer = segments.stream()
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

        // Executes only a local reservation.
        if (segmentsByServer.size() == 1 && hasLocalSegments) {
            return createLocalReservation(mySegmentsIds, user);
        }

        boolean success = coordinatorService.remoteCreateReservation(segmentsByServer, hasLocalSegments, TransactionType.CREATE_RESERVATION);

        if (!success) { return null;}
        Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), segments);
        try {
            reservationRepository.save(reservation);
            return reservation;
        } catch (IOException e) {
            throw new ReservationNotCreatedException("Os assentos foram confirmados mas houve uma falha ao persistir o registro da reserva." + e);
        }
    }

    private Reservation createLocalReservation(List<UUID> segmentIds, User user){
        List<ReentrantLock> locks = lockService.lock(segmentIds);

        try {
            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null || !transactionService.checkAvailability(currentSegments)) {
                throw new ReservationNotCreatedException("Não foi possível reservar o itinerário. Tente novamente mais tarde.");
            }

            Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), currentSegments);

            for (Segment segment : currentSegments) {
                segment.setAvailableSeats(segment.getAvailableSeats() - 1);
            }

            rideRepository.saveAll();
            reservationRepository.save(reservation);

            return reservation;
        } catch (IOException e){
            throw new ReservationNotCreatedException("Não foi possível reservar o itinerário. Tente novamente mais tarde.");
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public List<Reservation> listAllUserReservations(UUID userId){
        return reservationRepository.listAllUserReservations(userId);
    }

    public void delete(UUID id){
        Reservation reservation = reservationRepository.getById(id);
        if (reservation == null){
            throw new NotFoundException("Não foi possível encontrar essa reserva.");
        }

        Map<String, List<UUID>> segmentsByServer = reservation.segments().stream()
                .collect(Collectors.groupingBy(
                        Segment::getServerId,
                        Collectors.mapping(
                                Segment::getId,
                                Collectors.toList()
                        )
                ));

        List<UUID> mySegmentsIds = segmentsByServer.get(peerProperties.selfId());
        // If the reservation contains local segments.
        boolean hasLocalSegments = mySegmentsIds != null;

        // Only local reservation, so executes only local delete.
        if (segmentsByServer.size() == 1 && hasLocalSegments) {
            deleteLocalReservation(reservation);
            return;
        }

        boolean success = coordinatorService.remoteCreateReservation(segmentsByServer, hasLocalSegments, TransactionType.CANCEL_RESERVATION);

        if (!success) { return; }
        try {
            reservationRepository.delete(reservation.id());
            reservationRepository.save(reservation);
        } catch (IOException e) {
            throw new ReservationNotCreatedException("Os assentos foram confirmados mas houve uma falha ao persistir o registro da reserva." + e);
        }
    }

    private void deleteLocalReservation (Reservation reservation){
        List<UUID> reservedSegments = reservation.segments().stream().map(Segment::getId).sorted().toList();
        List<ReentrantLock> locks = lockService.lock(reservedSegments);

        try {
            rideRepository.releaseSeatsInSegments(reservedSegments);
            reservationRepository.delete(reservation.id());
        } catch (IOException e) {
            throw new PersistenceErrorException("Não foi possível fazer cancelar a reserva.", e);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public List<Reservation> getReservationsBySegments(List<UUID>segmentIds){
        return reservationRepository.findReservationBySegments(segmentIds);
    }

}
