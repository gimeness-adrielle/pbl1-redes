package me.gimenez.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.ItineraryDTO;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.Response;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class PassengerClient {
    private final Client client;
    private final ObjectMapper mapper;

    public List<ItineraryDTO.Response> searchItinerary(String origin, String destination, LocalDate date) {
        ItineraryDTO.Request request = new ItineraryDTO.Request(origin, destination, date);

        Response response = client.sendRequest("SEARCH_ITINERARY", request);

        return mapper.convertValue(response.data(), new TypeReference<>() {});
    }

    public Response reserveItinerary (List<Segment> segments){
        ReservationDTO.Request request = new ReservationDTO.Request(segments);

        return client.sendRequest("RESERVE_ITINERARY", request);
    }

    public List<Reservation> listReservations (){
        Response response = client.sendRequest("LIST_RESERVATIONS", null);
        return mapper.convertValue(response.data(), new TypeReference<>() {});
    }

    public Response deleteReservation (UUID reservationId){
            return client.sendRequest("DELETE_RESERVATION", reservationId);
    }
}
