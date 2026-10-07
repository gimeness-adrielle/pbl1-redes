package me.gimenez.server.services;

import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.server.repository.ReservationRepository;
import me.gimenez.server.repository.RideRepository;
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

    public Reservation reserve(List<UUID> segmentIds, User user){
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

            boolean haveAvailableSeats = currentSegments.stream().allMatch(segment -> segment.getAvailableSeats() > 0);
            if (!haveAvailableSeats) {
                throw new ReservationNotCreatedException("Não há assentos disponíveis para todos os segmentos.");
            }

            Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), currentSegments);

            reservationRepository.save(reservation);
            for (Segment segment : currentSegments) {
                segment.setAvailableSeats(segment.getAvailableSeats() - 1);
            }
            rideRepository.saveAll();

            return reservation;
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
        List<UUID> reservedSegments = reservation.segments().stream().map(Segment::getId).sorted().toList();

        List<ReentrantLock> locks = reservedSegments.stream().map(this::getLock).toList();
        locks.forEach(ReentrantLock::lock);

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
