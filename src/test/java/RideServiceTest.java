import me.gimenez.model.Itinerary;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.SearchRideRequest;
import me.gimenez.services.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RideServiceTest {

    @Mock
    private RideRepository repository;

    @InjectMocks
    private RideService service;

    @Test
    public void should_return_oneItinerary_with_oneSegment() throws IOException {
        SearchRideRequest request = new SearchRideRequest("Feira de Santana", "Salvador", LocalDate.of(2026, 9, 15));

        Segment segment = new Segment(UUID.randomUUID(), "Feira de Santana", "Salvador", 60, 3);

        Ride ride = new Ride(
                UUID.randomUUID(),
                List.of("Feira de Santana", "Salvador"),
                LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 30),
                List.of(segment));

        when(repository.findAllRides()).thenReturn(List.of(ride));

        List<Itinerary> itineraries = service.search(request);

        assertEquals(1, itineraries.size());
        assertEquals(1, itineraries.getFirst().segments().size());
        assertEquals("Feira de Santana", itineraries.getFirst().segments().getFirst().getOrigin());
        assertEquals("Salvador", itineraries.getFirst().segments().getFirst().getDestination());
    }

    @Test
    public void should_return_oneItinerary_with_twoSegments() throws IOException {
        SearchRideRequest request = new SearchRideRequest("Salvador", "Vitória da Conquista", LocalDate.of(2026, 9, 15));

        Segment segment1 = new Segment(UUID.randomUUID(), "Feira de Santana", "Vitória da Conquista", 60, 2);
        Segment segment2 = new Segment(UUID.randomUUID(), "Salvador", "Feira de Santana", 60, 1);

        Ride ride1 = new Ride(
                UUID.randomUUID(),
                List.of("Feira de Santana", "Vitória da Conquista"),
                LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 30),
                List.of(segment1));
        Ride ride2 = new Ride(
                UUID.randomUUID(),
                List.of("Salvador", "Feira de Santana"),
                LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 30),
                List.of(segment2));

        List<Ride> rides =  List.of(ride1, ride2);

        when(repository.findAllRides()).thenReturn(rides);

        List<Itinerary> itineraries = service.search(request);

        assertEquals(1, itineraries.size());
        assertEquals(2, itineraries.getFirst().segments().size());
        assertEquals("Salvador", itineraries.getFirst().segments().getFirst().getOrigin());
        assertEquals("Vitória da Conquista",  itineraries.getFirst().segments().getLast().getDestination());
    }

    @Test
    public void should_return_emptyItineraries_when_segment_has_no_available_seats() throws IOException {
        SearchRideRequest request = new SearchRideRequest("Feira de Santana", "Salvador", LocalDate.of(2026, 9, 15));

        // Dois trechos, mas o desejado não tem vagas disponíveis.
        Segment segment1 = new Segment(UUID.randomUUID(), "Feira de Santana", "Camaçari", 60, 2);
        Segment segment2 = new Segment(UUID.randomUUID(), "Camaçari", "Salvador", 60, 0);

        Ride ride = new Ride(
                UUID.randomUUID(),
                List.of("Feira de Santana", "Camaçari", "Salvador"),
                LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 30),
                List.of(segment1, segment2));

        when(repository.findAllRides()).thenReturn(List.of(ride));

        List<Itinerary> itineraries = service.search(request);

        assertEquals(0, itineraries.size());
    }

    @Test
    public void should_return_nothing_with_impossibleItinerary() throws IOException {
        SearchRideRequest request = new SearchRideRequest("Salvador", "São Paulo", LocalDate.of(2026, 9, 15));

        Segment segment1 = new Segment(UUID.randomUUID(), "Salvador", "Camaçari", 60, 2);
        Segment segment2 = new Segment(UUID.randomUUID(), "Camaçari", "Feira de Santana", 60, 1);

        Ride ride = new Ride(
                UUID.randomUUID(),
                List.of("Salvador", "Camaçari", "Feira de Santana"),
                LocalDate.of(2026, 9, 15),
                LocalTime.of(10, 30),
                List.of(segment1, segment2));

        when(repository.findAllRides()).thenReturn(List.of(ride));

        List<Itinerary> itineraries = service.search(request);

        assertEquals(0, itineraries.size());

    }

}
