package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Ride;
import me.gimenez.model.users.User;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.*;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;
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


        this.clientSocket = clientSocket;
        this.rideService = new RideService(new RideRepository(mapper));
        this.userService = new UserService();
        this.reservationService = new ReservationService(new ReservationRepository(mapper));
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

    public void sendResponse (Response response) throws IOException {
        String json = mapper.writeValueAsString(response);
        out.println(json);
    }

    public void handleRequest(Request request) throws IOException {
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
        }
    }

    public void handlePublishRide(Request request) throws IOException {
        PublishRideRequest publishRequest = mapper.convertValue(request.data(), PublishRideRequest.class);
        Ride ride = rideService.publish(publishRequest, currentUser);

        if (ride == null){
            sendResponse(new Response("ERROR", "Não foi possível publicar a carona.", null));
            return;
        }

        sendResponse(new Response("OK", "Carona publicada com sucesso!", ride));
    }

    public void handleLogin (Request request) throws IOException {
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

    public void handleRegister (Request request) throws IOException {
        RegisterRequest registerRequest = mapper.convertValue(request.data(), RegisterRequest.class);
        userService.register(registerRequest);
        sendResponse(new Response("CREATED", "Nova conta registrada com sucesso.", null));
    }

    public void handleSearchRides(Request request) throws IOException {
        SearchRideRequest searchRequest = mapper.convertValue(request.data(), SearchRideRequest.class);
        List<Itinerary> itineraries = rideService.search(searchRequest);

        if (itineraries.isEmpty()){
            sendResponse(new Response("NOT_FOUND", "Não foram encontrados itinerários.", null));
            return;
        }

        sendResponse(new Response("OK", "Busca realizada com sucesso.", itineraries));
    }

    public void handleReserveItinerary(Request request) throws IOException {
        ReservationRequest reserveRequest = mapper.convertValue(request.data(), ReservationRequest.class);
        boolean success = rideService.reserveSegments(reserveRequest.segmentsIds());
        boolean reserved = reservationService.reserve(reserveRequest.itinerary(), currentUser);

        if (success && reserved){
            sendResponse(new Response("OK", "Reserva realizada com sucesso.", null));
        } else {
            sendResponse(new Response("ERROR", "Não foi possível realizar a reserva.", null));
        }
    }

}

