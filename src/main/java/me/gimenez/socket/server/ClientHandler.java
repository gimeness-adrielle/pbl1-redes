package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.users.User;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.PublishRideRequest;
import me.gimenez.requests.Request;
import me.gimenez.requests.SearchRideRequest;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;
import me.gimenez.services.RideService;
import me.gimenez.services.UserService;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
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
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String json;

            while ((json = in.readLine()) != null) {
                Request request = mapper.readValue(json, Request.class);

                switch (request.type()){
                    case "PUBLISH_RIDE":
                        PublishRideRequest publishRequest = mapper.convertValue(request.data(), PublishRideRequest.class);
                        rideService.publish(publishRequest, currentUser);
                        break;
                    case "LOGIN":
                        LoginRequest loginRequest = mapper.convertValue(request.data(), LoginRequest.class);
                        try{
                            currentUser = userService.login(loginRequest);
                            System.out.println("Entrou como: " + currentUser.username());

                            out.println(mapper.writeValueAsString(currentUser));
                        } catch (RuntimeException e) {
                            out.println("LOGIN_ERROR");
                        }
                        break;
                    case "REGISTER":
                        RegisterRequest registerRequest = mapper.convertValue(request.data(), RegisterRequest.class);
                        userService.register(registerRequest);
                        System.out.println("Nova conta registrada com sucesso!");
                        break;
                    case "SEARCH_RIDES":
                        SearchRideRequest searchRequest = mapper.convertValue(request.data(), SearchRideRequest.class);
                        List<Itinerary> itineraries = rideService.search(searchRequest);
                        out.println(mapper.writeValueAsString(itineraries));
                        break;
                }
            }

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

