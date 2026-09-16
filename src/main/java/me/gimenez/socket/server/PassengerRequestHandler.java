package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.dto.Request;
import me.gimenez.dto.Response;
import me.gimenez.dto.reservation.CreateReservationRequest;
import me.gimenez.dto.reservation.DeleteReservationRequest;
import me.gimenez.dto.ride.SearchRideRequest;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.users.User;
import me.gimenez.services.ReservationService;
import me.gimenez.services.RideService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class PassengerRequestHandler {
    private final PrintWriter out;
    private final ObjectMapper mapper;
    private final RideService rideService;
    private final ReservationService reservationService;

    private final User user;

    public PassengerRequestHandler(ObjectMapper mapper,
                                PrintWriter out,
                                RideService rideService,
                                ReservationService reservationService,
                                User user) {
        this.out = out;
        this.mapper = mapper;
        this.rideService = rideService;
        this.reservationService = reservationService;
        this.user = user;
    }

    public void handle(Request request) throws IOException {
        switch (request.type()){
            case "SEARCH_RIDES":
                handleSearchRides(request);
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

    void sendResponse (Response response) throws IOException {
        String json = mapper.writeValueAsString(response);
        out.println(json);
    }

    void handleSearchRides(Request request) throws IOException {
        SearchRideRequest searchRequest = mapper.convertValue(request.data(), SearchRideRequest.class);
        List<Itinerary> itineraries = rideService.search(searchRequest);

        if (itineraries.isEmpty()){
            sendResponse(new Response("NOT_FOUND", "Não foram encontrados itinerários.", null));
            return;
        }

        sendResponse(new Response("OK", "Busca realizada com sucesso.", itineraries));
    }

    void handleReserveItinerary(Request request) throws IOException {
        CreateReservationRequest createReservationRequest = mapper.convertValue(request.data(), CreateReservationRequest.class);
        Reservation reservation = reservationService.reserve(createReservationRequest.itinerary(), createReservationRequest.segmentsIds(), user);

        if (reservation != null){
            sendResponse(new Response("OK", "Reserva realizada com sucesso.", null));
        } else {
            sendResponse(new Response("ERROR", "Não foi possível realizar a reserva.", null));
        }
    }

    void handleListReservations() throws IOException {
        List<Reservation> reservations = reservationService.listAllUserReservations(user.id());
        sendResponse(new Response("OK", "Busca realizada com sucesso.", reservations));
    }

    void handleDeleteReservation(Request request) throws IOException {
        boolean success = reservationService.delete(mapper.convertValue(request.data(), DeleteReservationRequest.class).id());

        if (success){
            sendResponse(new Response("DELETED", "A reserva foi deletada realizada com sucesso.", null));
        }else{
            sendResponse(new Response("ERROR", "Não foi possível deletar a reserva.", null));
        }
    }
}
