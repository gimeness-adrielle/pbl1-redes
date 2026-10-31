package me.gimenez.server.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.domain.dto.ItineraryDTO;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.Request;
import me.gimenez.domain.dto.Response;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.User;
import me.gimenez.server.reservations.ReservationService;
import me.gimenez.server.rides.RideService;

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
        ItineraryDTO.Request itineraryRequest = mapper.convertValue(request.data(), ItineraryDTO.Request.class);
        List<ItineraryDTO.Response> itineraries = rideService.searchItinerary(itineraryRequest);
        clientHandler.sendResponse(new Response("OK", "Busca realizada com sucesso.", itineraries));
    }

    void handleReserveItinerary(Request request){
        ReservationDTO.Request reservationRequest = mapper.convertValue(request.data(), ReservationDTO.Request.class);

        try {
            reservationService.createReservation(reservationRequest.segments(), user);
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
