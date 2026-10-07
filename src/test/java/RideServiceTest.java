
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Segment;
import me.gimenez.server.repository.RideRepository;
import me.gimenez.domain.dto.requests.ItineraryRequest;
import me.gimenez.server.services.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void shouldReturnOneItinerary_whenHasOneSegment() {
        LocalDateTime departureAt = LocalDateTime.of(2026, 9, 15, 10, 0);
        LocalDateTime arrivalAt = LocalDateTime.of(2026, 9, 15, 11, 30);

        Segment segment = new Segment(
                UUID.randomUUID(), null,
                "Feira de Santana", "Salvador",
                60, 3,
                departureAt, arrivalAt
        );

        LocalDate searchDate = LocalDate.of(2026, 9, 15);
        ItineraryRequest request = new ItineraryRequest("Feira de Santana", "Salvador", searchDate);

        when(repository.searchItinerary(searchDate)).thenReturn(List.of(segment));

        List<ItineraryResponse> itineraries = service.searchItinerary(request);

        assertEquals(1, itineraries.size());
        assertEquals(1, itineraries.getFirst().segments().size());
        assertEquals("Feira de Santana", itineraries.getFirst().segments().getFirst().getOrigin());
        assertEquals("Salvador", itineraries.getFirst().segments().getFirst().getDestination());
    }

    @Test
    void shouldReturnOneItinerary_whenHasTwoSegments() throws IOException {
        LocalDateTime departureAt = LocalDateTime.of(2026, 9, 15, 10, 0);
        LocalDateTime arrivalAt1 = LocalDateTime.of(2026, 9, 15, 11, 30);
        LocalDateTime arrivalAt2 = LocalDateTime.of(2026, 9, 15, 13, 30);

        Segment segment1 = new Segment(
                UUID.randomUUID(), null,
                "Salvador", "Feira de Santana",
                60, 3,
                departureAt, arrivalAt1
        );
        Segment segment2 = new Segment(
                UUID.randomUUID(), null,
                "Feira de Santana", "Vitória da Conquista",
                60, 3,
                arrivalAt1, arrivalAt2
        );

        LocalDate searchDate = LocalDate.of(2026, 9, 15);
        ItineraryRequest request = new ItineraryRequest("Salvador", "Vitória da Conquista", searchDate);

        when(repository.searchItinerary(searchDate)).thenReturn(List.of(segment1, segment2));
        List<ItineraryResponse> itineraries = service.searchItinerary(request);

        assertEquals(1, itineraries.size());
        assertEquals(2, itineraries.getFirst().segments().size());
        assertEquals("Salvador", itineraries.getFirst().segments().getFirst().getOrigin());
        assertEquals("Vitória da Conquista",  itineraries.getFirst().segments().getLast().getDestination());
    }

    @Test
    void shouldNotReturnItinerary_whenDateTimeIsNotAligned(){
        LocalDateTime departureAt = LocalDateTime.of(2026, 9, 15, 10, 0);
        LocalDateTime arrivalAt = LocalDateTime.of(2026, 9, 15, 11, 30);

        Segment segment1 = new Segment(
                UUID.randomUUID(), null,
                "Salvador", "Feira de Santana",
                60, 3,
                departureAt, arrivalAt
        );
        Segment segment2 = new Segment(
                UUID.randomUUID(), null,
                "Feira de Santana", "Vitória da Conquista",
                60, 3,
                departureAt, arrivalAt
        );

        LocalDate searchDate = LocalDate.of(2026, 9, 15);
        ItineraryRequest request = new ItineraryRequest("Salvador", "Vitória da Conquista", searchDate);

        when(repository.searchItinerary(searchDate)).thenReturn(List.of(segment1, segment2));
        List<ItineraryResponse> itineraries = service.searchItinerary(request);

        assertEquals(0, itineraries.size());
    }

}
