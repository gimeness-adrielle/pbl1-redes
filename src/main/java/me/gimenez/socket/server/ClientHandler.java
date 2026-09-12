package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.users.User;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.*;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;
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
    private User currentUser;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.rideService = new RideService(new RideRepository());
        this.userService = new UserService();
        this.mapper = new ObjectMapper();
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

    public void handlePublishRide(Request request){
        PublishRideRequest publishRequest = mapper.convertValue(request.data(), PublishRideRequest.class);
        rideService.publish(publishRequest, currentUser);
    }

    public void handleLogin (Request request) throws IOException {
        LoginRequest loginRequest = mapper.convertValue(request.data(), LoginRequest.class);
        try{
            currentUser = userService.login(loginRequest);
            out.println(mapper.writeValueAsString(currentUser));

        } catch (IOException e) {
            sendResponse(new Response("400", e.getMessage(), null));
        }
    }

    public void handleRegister (Request request) throws IOException {
        RegisterRequest registerRequest = mapper.convertValue(request.data(), RegisterRequest.class);
        userService.register(registerRequest);
        sendResponse(new Response("201", "Nova conta registrada com sucesso.", null));
    }

    public void handleSearchRides(Request request) throws IOException {
        SearchRideRequest searchRequest = mapper.convertValue(request.data(), SearchRideRequest.class);
        List<Itinerary> itineraries = rideService.search(searchRequest);
        out.println(mapper.writeValueAsString(itineraries));
    }

    public void handleReserveItinerary(Request request) throws IOException {
        ReserveItineraryRequest reserveRequest = mapper.convertValue(request.data(), ReserveItineraryRequest.class);
        boolean success = rideService.reserveItinerary(reserveRequest.segmentsIds());

        if (success){
            sendResponse(new Response("200", "Reserva realizada com sucesso.", null));
        } else {
            sendResponse(new Response("400", "Não foi possível realizar a reserva.", null));
        }
    }

}

