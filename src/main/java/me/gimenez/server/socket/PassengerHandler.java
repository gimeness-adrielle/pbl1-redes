package me.gimenez.server.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.domain.dto.requests.Request;
import me.gimenez.domain.dto.requests.ReservationRequest;
import me.gimenez.domain.dto.responses.Response;
import me.gimenez.domain.dto.requests.ItineraryRequest;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.User;
import me.gimenez.server.services.ReservationService;
import me.gimenez.server.services.RideService;

import java.util.List;
import java.util.UUID;

public class PassengerHandler {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;

    private final RideService rideService;
    private final ReservationService reservationService;

    private final User user;

    public PassengerHandler(ObjectMapper mapper,
                            ClientHandler clientHandler,
                            RideService rideService,
                            ReservationService reservationService,
                            User user) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.rideService = rideService;
        this.reservationService = reservationService;
        this.user = user;
    }

    public void handle(Request request) {
        switch (request.type()){
            case "SEARCH_ITINERARY":
                handleSearchItinerary(request);
                break;

            case "RESERVE_ITINERARY":
                handleReserveItinerary(request);
                break;

            case "LIST_RESERVATIONS":
                handleListReservations();
                break;

            case "DELETE_RESERVATION":
                handleDeleteReservation(request);
        }
    }

    void handleSearchItinerary(Request request) {
        ItineraryRequest itineraryRequest = mapper.convertValue(request.data(), ItineraryRequest.class);
        List<ItineraryResponse> itineraries = rideService.searchItinerary(itineraryRequest);
        clientHandler.sendResponse(new Response("OK", "Busca realizada com sucesso.", itineraries));
    }

    void handleReserveItinerary(Request request){
        ReservationRequest reservationRequest = mapper.convertValue(request.data(), ReservationRequest.class);

        try {
            reservationService.reserve(reservationRequest.segmentsIds(), user);
        } catch (ReservationNotCreatedException e){
            clientHandler.sendResponse(Response.error(e.getMessage()));
            return;
        }
        clientHandler.sendResponse(Response.ok("Reserva realizada com sucesso!", null));
    }

    void handleListReservations() {
        List<Reservation> reservations = reservationService.listAllUserReservations(user.id());
        clientHandler.sendResponse(new Response("OK", "Busca realizada com sucesso.", reservations));
    }

    void handleDeleteReservation(Request request) {
        try {
            reservationService.delete(mapper.convertValue(request.data(), UUID.class));
        } catch (PersistenceErrorException e){
            clientHandler.sendResponse(Response.error(e.getMessage()));
            return;
        }

        clientHandler.sendResponse(Response.ok("Sua reserva foi cancelada com sucesso!", null));
    }
}
