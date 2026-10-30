package me.gimenez.server.reservations;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.models.*;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.server.rides.RideRepository;
import me.gimenez.server.transactions.TransactionCoordinatorService;
import me.gimenez.server.transactions.TransactionService;
import me.gimenez.util.LockService;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
            return localReservation(mySegmentsIds, user);
        }

        coordinatorService.remoteReservation(segmentsByServer, hasLocalSegments);

        Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), segments);
        try {
            reservationRepository.save(reservation);
            return reservation;
        } catch (IOException e) {
            throw new ReservationNotCreatedException("Os assentos foram confirmados mas houve uma falha ao persistir o registro da reserva." + e);
        }
    }

    private Reservation localReservation(List<UUID> segmentIds, User user){
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
        List<UUID> reservedSegments = reservation.segments().stream().map(Segment::getId).sorted().toList();
        List<ReentrantLock> locks = lockService.lock(reservedSegments);

        try {
            rideRepository.releaseSeatsInSegments(reservedSegments);
            reservationRepository.delete(id);
        } catch (IOException e) {
            throw new PersistenceErrorException("Não foi possível fazer cancelar a reserva.", e);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

}
