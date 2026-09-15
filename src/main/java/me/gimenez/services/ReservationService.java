package me.gimenez.services;

import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Segment;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    public Reservation reserve(Itinerary itinerary, List<UUID> segmentIds, User user){
        // Locks
        List<UUID> sortedSegmentIds = segmentIds.stream().sorted().toList();    // Ordena os IDs para resolver deadlocks
        List<ReentrantLock> locks = sortedSegmentIds.stream().map(this::getLock).toList();

        for (ReentrantLock lock : locks) {
            System.out.println("Thread: " + Thread.currentThread().getName() + " Trying to reserve " + lock.toString());
            lock.lock();
            System.out.println("Thread: " + Thread.currentThread().getName() + " Trying to reserve " + lock);
        }
        //

        try {
            // Lista de segmentos verdadeiros do sistema: não é correto confiar na lista enviada no request, pois no
            // sistema pode ter mudado dados do segmento, e a lista do request vai estar desatualizada.
            List<Segment> currentSegments = new ArrayList<>();

            // Encontra os segmentos do request no repositório
            for (UUID segmentId : segmentIds) {
                Segment segment = rideRepository.findSegmentById(segmentId);
                System.out.println("Thread: " + Thread.currentThread().getName() + " Trying to reserve " + segment.toString());
                System.out.println("Vagas: " + segment.getAvailableSeats());
                if (segment.getAvailableSeats() <= 0){
                    System.out.println("Thread: " + Thread.currentThread().getName() + " Segmento: " +
                            segment.getOrigin() + " SEM VAGA");
                    return null;
                }

                currentSegments.add(segment);
            }

            Reservation reservation = new Reservation(UUID.randomUUID(), user.id(), itinerary);

            reservationRepository.save(reservation);
            for (Segment segment : currentSegments) {
                segment.setAvailableSeats(segment.getAvailableSeats() - 1);
            }
            rideRepository.saveAll();

            return reservation;
        } catch (IOException e){
            return null;
        } finally {
            locks.forEach((lock) -> {
                System.out.println("Thread: " + Thread.currentThread().getName() + " Liberando " + lock.toString());
                lock.unlock();
            });
        }
    }

    public List<Reservation> listAllUserReservations(UUID userId){
        return reservationRepository.listAllUserReservations(userId);
    }

    public boolean delete(UUID id){
        Reservation reservation = reservationRepository.getById(id);
        if(reservation == null){
            return false;
        }

        List<Segment> segments = reservation.itinerary().segments();

        try {
            for(Segment segment : segments){
                segment.setAvailableSeats(segment.getAvailableSeats() + 1);
            }
            rideRepository.saveAll();

            return reservationRepository.delete(id);
        } catch (IOException e) {
            return false;
        }
    }

}
