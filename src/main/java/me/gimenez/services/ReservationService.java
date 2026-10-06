package me.gimenez.services;

import me.gimenez.exceptions.PersistenceErrorException;
import me.gimenez.exceptions.ReservationNotCreatedException;
import me.gimenez.model.Reservation;
import me.gimenez.model.Segment;
import me.gimenez.model.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RideRepository rideRepository;
    private final Map<UUID, ReentrantLock> segmentLocks = new ConcurrentHashMap<>();

    public ReservationService(ReservationRepository reservationRepository, RideRepository rideRepository) {
        this.reservationRepository = reservationRepository;
        this.rideRepository = rideRepository;
    }

    private ReentrantLock getLock (UUID segmentId){
        return segmentLocks.computeIfAbsent(segmentId, id -> new ReentrantLock());
    }

    public void reserve(List<UUID> segmentIds, User user){
        // Locks
        List<UUID> sortedSegmentIds = segmentIds.stream().sorted().toList();    // Ordena os IDs para resolver deadlocks
        List<ReentrantLock> locks = sortedSegmentIds.stream().map(this::getLock).toList();

        for (ReentrantLock lock : locks) {
            lock.lock();
        }
        //

        try {
            // Lista de segmentos verdadeiros do sistema: não é correto confiar na lista enviada no request, pois no
            // sistema pode ter mudado dados do segmento, e a lista do request vai estar desatualizada.
            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null || currentSegments.isEmpty()) {
                throw new ReservationNotCreatedException("Não foi possível reservar o itinerário. Tente novamente mais tarde.");
            }

            Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), currentSegments);

            reservationRepository.save(reservation);
            for (Segment segment : currentSegments) {
                segment.setAvailableSeats(segment.getAvailableSeats() - 1);
            }
            rideRepository.saveAll();
        } catch (IOException e){
            throw new ReservationNotCreatedException("Não foi possível reservar o itinerário. Tente novamente mais tarde." + e.getMessage());
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public List<Reservation> listAllUserReservations(UUID userId){
        return reservationRepository.listAllUserReservations(userId);
    }

    public void delete(UUID id){
        Reservation reservation = reservationRepository.getById(id);
        List<UUID> reservedSegments = reservation.segments().stream().map(Segment::getId).toList();

        try {
            rideRepository.deleteReserveInSegment(reservedSegments);
            reservationRepository.delete(id);
        } catch (IOException e) {
            throw new PersistenceErrorException("Não foi possível fazer cancelar a reserva.", e);
        }
    }

}
