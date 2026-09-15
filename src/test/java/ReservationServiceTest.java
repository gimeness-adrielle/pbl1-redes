import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Segment;
import me.gimenez.model.users.User;
import me.gimenez.model.users.UserType;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.services.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private RideRepository rideRepository;
    @InjectMocks
    private ReservationService service;

    private User user;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        this.user = new User(userId, "Adrielle", "adrielle255", "12345", UserType.PASSENGER);
    }

    // Fazer reserva SEM concorrência
    @Test
    void should_reserve_one_segment() throws IOException {
        UUID segmentId = UUID.randomUUID();

        Segment segment = new Segment(segmentId, "Feira de Santana", "Salvador", 50.0, 2);

        Itinerary itinerary = new Itinerary(50.0, List.of(segment));

        when(rideRepository.findSegmentById(segmentId)).thenReturn(segment);

        Reservation reservation = service.reserve(itinerary, List.of(segmentId), user);

        assertNotNull(reservation);
        assertEquals(1, segment.getAvailableSeats());

        verify(reservationRepository).save(any(Reservation.class));
        verify(rideRepository).saveAll();
    }

    @Test
    void should_reserve_all_segments() throws IOException {
        UUID segmentId1 = UUID.randomUUID();
        UUID segmentId2 = UUID.randomUUID();

        Segment segment1 = new Segment(segmentId1, "Feira de Santana", "Salvador", 50.0, 2);
        Segment segment2 = new Segment(segmentId2, "Salvador", "Aracaju", 80.0, 3);

        Itinerary itinerary = new Itinerary(130.0, List.of(segment1, segment2));

        when(rideRepository.findSegmentById(segmentId1)).thenReturn(segment1);

        when(rideRepository.findSegmentById(segmentId2)).thenReturn(segment2);

        Reservation reservation = service.reserve(itinerary, List.of(segmentId1, segmentId2), user);

        assertNotNull(reservation);
        assertEquals(1, segment1.getAvailableSeats());
        assertEquals(2, segment2.getAvailableSeats());

        verify(reservationRepository).save(any(Reservation.class));
        verify(rideRepository).saveAll();
    }

    @Test
    void should_not_reserve_when_one_segment_has_no_seats() throws IOException {
        UUID segmentId1 = UUID.randomUUID();
        UUID segmentId2 = UUID.randomUUID();

        Segment segment1 = new Segment(segmentId1, "Feira de Santana", "Salvador", 50.0, 2);
        Segment segment2 = new Segment(segmentId2, "Salvador", "Aracaju", 80.0, 0);

        Itinerary itinerary = new Itinerary(130.0, List.of(segment1, segment2));

        when(rideRepository.findSegmentById(segmentId1)).thenReturn(segment1);

        when(rideRepository.findSegmentById(segmentId2)).thenReturn(segment2);

        Reservation reservation = service.reserve(itinerary, List.of(segmentId1, segmentId2), user);

        assertNull(reservation);
        assertEquals(2, segment1.getAvailableSeats());
        assertEquals(0, segment2.getAvailableSeats());

        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(rideRepository, never()).saveAll();
    }

    // Deletar reserva SEM concorrência
    @Test
    void should_delete_reservation() throws IOException {
        UUID segmentId = UUID.randomUUID();

        Segment segment = new Segment(segmentId, "Feira de Santana", "Salvador", 60.0, 3);

        Itinerary itinerary = new Itinerary(60.0, List.of(segment));

        when(rideRepository.findSegmentById(segmentId)).thenReturn(segment);

        Reservation reservation = service.reserve(itinerary, List.of(segmentId), user);

        assertEquals(2, segment.getAvailableSeats());

        when(reservationRepository.getById(reservation.id())).thenReturn(reservation);

        service.delete(reservation.id());

        assertEquals(3, segment.getAvailableSeats());

        verify(reservationRepository).save(any(Reservation.class));
        verify(rideRepository, times(2)).saveAll();
    }

    // Fazer reserva COM concorrência
    @Test
    void should_reserve_oneSegment_with_concurrence() throws Exception {
        UUID segmentId = UUID.randomUUID();
        Segment segment = new Segment(segmentId, "Feira de Santana", "Salvador", 60.0, 1);
        Itinerary itinerary = new Itinerary(60.0, List.of(segment));

        when(rideRepository.findSegmentById(segmentId)).thenReturn(segment);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Reservation>> futures = new ArrayList<>();
        CountDownLatch latch = new  CountDownLatch(1);

        for (int i = 1; i <= 10; i++) {
            futures.add(executorService.submit(() -> {
                User user = new User(UUID.randomUUID(), "name", "username", "password", UserType.PASSENGER);
                latch.await();
                return service.reserve(itinerary, List.of(segmentId), user);
            }));
        }

        latch.countDown();

        int successful = 0;
        int failed = 0;

        for (Future<Reservation> future : futures) {
            if (future.get() != null){
                successful++;
            }else {
                failed++;
            }
        }
        executorService.shutdown();

        assertEquals(1, successful);
        assertEquals(9, failed);
        assertEquals(0, segment.getAvailableSeats());
    }

    @Test
    void should_reserve_segments_with_concurrence() throws Exception {
        UUID segmentId1 = UUID.randomUUID();
        UUID segmentId2 = UUID.randomUUID();
        UUID segmentId3 = UUID.randomUUID();

        Segment segment1 = new Segment(segmentId1, "Salvador", "Feira de Santana", 60, 1);
        Segment segment2 = new Segment(segmentId2, "Feira de Santana", "Itaberaba", 80, 1);
        Segment segment3 = new Segment(segmentId3, "Itaberaba", "Vitoria da Conquista", 100, 1);

        Itinerary itinerary = new Itinerary(240.0, List.of(segment1, segment2, segment3));

        when(rideRepository.findSegmentById(segmentId1)).thenReturn(segment1);
        when(rideRepository.findSegmentById(segmentId2)).thenReturn(segment2);
        when(rideRepository.findSegmentById(segmentId3)).thenReturn(segment3);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Reservation>> futures = new ArrayList<>();
        CountDownLatch latch = new  CountDownLatch(1);

        for (int i = 1; i <= 10; i++) {
            futures.add(executorService.submit(() -> {
                User user = new User(UUID.randomUUID(), "name", "username", "password", UserType.PASSENGER);
                latch.await();
                System.out.println(
                        "Thread: " + Thread.currentThread().getName()
                );
                Reservation reservation = service.reserve(itinerary, List.of(segmentId1, segmentId2, segmentId3), user);
                System.out.println(
                        Thread.currentThread().getName()
                                + " → "
                                + (reservation != null ? "SUCESSO" : "FALHA")
                );
                return reservation;
            }));
        }

        latch.countDown();

        int successful = 0;
        int failed = 0;

        for (Future<Reservation> future : futures) {
            if (future.get() != null){
                successful++;
            }else {
                failed++;
            }
        }
        executorService.shutdown();

        assertEquals(1, successful);
        assertEquals(9, failed);
        assertEquals(0, segment1.getAvailableSeats());
        assertEquals(0, segment2.getAvailableSeats());
        assertEquals(0, segment3.getAvailableSeats());
    }

}
