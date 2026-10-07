import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.domain.models.UserType;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.server.repository.ReservationRepository;
import me.gimenez.server.repository.RideRepository;
import me.gimenez.server.services.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        this.user = new User(userId, "passenger@gmail.com", "passenger", "12345678", UserType.PASSENGER);
    }

    // The tests below create reservations WITHOUT concurrency handling.
    @Test
    void should_reserve_all_segments() throws IOException {
        UUID segmentId1 = UUID.randomUUID();
        LocalDateTime departureAt1 = LocalDateTime.now().plusDays(2);
        LocalDateTime arrivalAt1 = departureAt1.plusHours(1);

        UUID segmentId2 = UUID.randomUUID();
        LocalDateTime arrivalAt2 = arrivalAt1.plusHours(2);

        Segment segment1 = new Segment(
                segmentId1, null,
                "Feira de Santana", "Salvador",
                50.0, 2,
                departureAt1, arrivalAt1
        );
        Segment segment2 = new Segment(
                segmentId2, null,
                "Salvador", "Aracaju",
                80.0, 3,
                arrivalAt1, arrivalAt2
        );
        List<Segment> segments = List.of(segment1, segment2);
        List<UUID> segmentsIds = List.of(segmentId1, segmentId2);

        when(rideRepository.findSegmentsById(segmentsIds)).thenReturn(segments);
        service.reserve(segmentsIds, user);

        assertEquals(1, segment1.getAvailableSeats());
        assertEquals(2, segment2.getAvailableSeats());

        verify(reservationRepository).save(any(Reservation.class));
        verify(rideRepository).saveAll();
    }

    @Test
    void should_not_reserve_when_one_segment_has_no_seats() throws IOException {
        UUID segmentId1 = UUID.randomUUID();
        UUID segmentId2 = UUID.randomUUID();

        Segment segment1 = new Segment(
                segmentId1, null,
                "Feira de Santana", "Salvador",
                50.0, 2,
                null, null
        );
        Segment segment2 = new Segment(
                segmentId2, null,
                "Salvador", "Aracaju",
                80.0, 0,
                null, null
        );

        List<Segment> segments = List.of(segment1, segment2);
        List<UUID> segmentsIds = List.of(segmentId1, segmentId2);

        when(rideRepository.findSegmentsById(segmentsIds)).thenReturn(segments);

        assertThrows(ReservationNotCreatedException.class, () -> service.reserve(segmentsIds, user));
        assertEquals(2, segment1.getAvailableSeats());
        assertEquals(0, segment2.getAvailableSeats());

        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(rideRepository, never()).saveAll();
    }

    // Tests to delete a reservation WITHOUT concurrency handling.
    @Test
    void should_delete_reservation() throws IOException {
        UUID segmentId = UUID.randomUUID();

        Segment segment = new Segment(
                segmentId, null,
                "Feira de Santana", "Salvador",
                60.0, 3,
                null, null
        );

        when(rideRepository.findSegmentsById(List.of(segmentId))).thenReturn(List.of(segment));

        Reservation reservation = service.reserve(List.of(segmentId), user);

        when(reservationRepository.getById(reservation.id())).thenReturn(reservation);
        service.delete(reservation.id());

        verify(reservationRepository).delete(reservation.id());
        verify(rideRepository).releaseSeatsInSegments(List.of(segmentId));
    }

    // The tests below create bookings WITH concurrency handling, simulating multiple users and bookings within
    // the same segments, using a thread pool and a countdown latch to ensure the bookings are executed simultaneously.
    @Test
    void should_reserve_oneSegment_with_concurrence() throws Exception {
        UUID segmentId = UUID.randomUUID();

        Segment segment = new Segment(
                segmentId, null,
                "Feira de Santana", "Salvador",
                60.0, 1,
                null, null
        );

        when(rideRepository.findSegmentsById(List.of(segmentId))).thenReturn(List.of(segment));

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Reservation>> futures = new ArrayList<>();
        CountDownLatch latch = new  CountDownLatch(1);

        for (int i = 1; i <= 10; i++) {
            futures.add(executorService.submit(() -> {
                User user = new User(UUID.randomUUID(), "name", "username", "password", UserType.PASSENGER);
                latch.await();
                return service.reserve(List.of(segmentId), user);
            }));
        }

        latch.countDown();
        int successful = 0; int failed = 0;

        for (Future<Reservation> future : futures) {
            try {
                if (future.get() != null){
                    successful++;
                }
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof ReservationNotCreatedException)) { throw e; }
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

        Segment segment1 = new Segment(
                segmentId1, null,
                "Feira de Santana", "Salvador",
                50.0, 2,
                null, null
        );
        Segment segment2 = new Segment(
                segmentId2, null,
                "Salvador", "Aracaju",
                80.0, 1,
                null, null
        );
        List<Segment> segments = List.of(segment1, segment2);
        List<UUID> segmentsIds = List.of(segmentId1, segmentId2);

        when(rideRepository.findSegmentsById(segmentsIds)).thenReturn(segments);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Reservation>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 1; i <= 10; i++) {
            futures.add(executorService.submit(() -> {
                User user = new User(UUID.randomUUID(), "name", "username", "password", UserType.PASSENGER);
                latch.await();
                return service.reserve(segmentsIds, user);
            }));
        }

        latch.countDown();
        int successful = 0; int failed = 0;

        for (Future<Reservation> future : futures) {
            try {
                if (future.get() != null){
                    successful++;
                }
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof ReservationNotCreatedException)) { throw e; }
                failed++;
            }
        }
        executorService.shutdown();

        assertEquals(1, successful);
        assertEquals(9, failed);
        assertEquals(1, segment1.getAvailableSeats());
        assertEquals(0, segment2.getAvailableSeats());
    }

}
