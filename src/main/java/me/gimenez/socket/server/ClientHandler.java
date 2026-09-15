package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.dto.reservation.CreateReservationRequest;
import me.gimenez.dto.reservation.DeleteReservationRequest;
import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.dto.ride.SearchRideRequest;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Ride;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.dto.*;
import me.gimenez.dto.auth.LoginRequest;
import me.gimenez.dto.auth.RegisterRequest;
import me.gimenez.services.ReservationService;
import me.gimenez.services.RideService;
import me.gimenez.services.UserService;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;

    private final ObjectMapper mapper;
    private final RideService rideService;
    private final UserService userService;
    private final ReservationService reservationService;
    private User currentUser;

    public ClientHandler(Socket clientSocket) {
        this.mapper = new ObjectMapper();
        RideRepository rideRepository = new RideRepository(mapper);

        this.clientSocket = clientSocket;
        this.rideService = new RideService(rideRepository);
        this.userService = new UserService();
        this.reservationService = new ReservationService(new ReservationRepository(mapper), rideRepository);
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run(){
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String json;

            while ((json = in.readLine()) != null) {
                Request request = mapper.readValue(json, Request.class);

                handleRequest(request);
            }

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendResponse (Response response) throws IOException {
        String json = mapper.writeValueAsString(response);
        out.println(json);
    }

    void handleRequest(Request request) throws IOException {
        switch (request.type()){
            case "PUBLISH_RIDE":
                handlePublishRide(request);
                break;

            case "LOGIN":
                handleLogin(request);
                break;

            case "REGISTER":
                handleRegister(request);
                break;

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

    void handlePublishRide(Request request) throws IOException {
        CreateRideRequest publishRequest = mapper.convertValue(request.data(), CreateRideRequest.class);
        Ride ride = rideService.publish(publishRequest, currentUser);

        if (ride == null){
            sendResponse(new Response("ERROR", "Não foi possível publicar a carona.", null));
            return;
        }

        sendResponse(new Response("OK", "Carona publicada com sucesso!", ride));
    }

    void handleLogin (Request request) throws IOException {
        LoginRequest loginRequest = mapper.convertValue(request.data(), LoginRequest.class);
        try{
            currentUser = userService.login(loginRequest);

            if (currentUser == null){
                sendResponse(new Response("ERROR", "Usuário ou senha incorretos.", null));
                return;
            }

            sendResponse(new Response(
                    "OK",
                    "Login realizado com sucesso!",
                    currentUser
            ));

        } catch (IOException e) {
            sendResponse(new Response("ERROR", e.getMessage(), null));
        }
    }

    void handleRegister (Request request) throws IOException {
        RegisterRequest registerRequest = mapper.convertValue(request.data(), RegisterRequest.class);
        userService.register(registerRequest);
        sendResponse(new Response("CREATED", "Nova conta registrada com sucesso.", null));
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
        Reservation reservation = reservationService.reserve(createReservationRequest.itinerary(), createReservationRequest.segmentsIds(), currentUser);

        if (reservation != null){
            sendResponse(new Response("OK", "Reserva realizada com sucesso.", null));
        } else {
            sendResponse(new Response("ERROR", "Não foi possível realizar a reserva.", null));
        }
    }

    void handleListReservations() throws IOException {
        List<Reservation> reservations = reservationService.listAllUserReservations(currentUser.id());
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

