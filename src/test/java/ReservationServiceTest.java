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
import java.util.List;
import java.util.UUID;

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

}
