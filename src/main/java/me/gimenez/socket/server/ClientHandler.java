package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.dto.reservation.CreateReservationRequest;
import me.gimenez.dto.reservation.DeleteReservationRequest;
import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.dto.ride.SearchRideRequest;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Ride;
import me.gimenez.model.users.User;
import me.gimenez.dto.*;
import me.gimenez.dto.auth.LoginRequest;
import me.gimenez.dto.auth.RegisterRequest;
import me.gimenez.model.users.UserType;
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
    private final ReservationService reservationService;
    private final UserService userService;

    private DriverRequestHandler driverRequestHandler;
    private PassengerRequestHandler passengerRequestHandler;

    private User currentUser;

    public ClientHandler(Socket clientSocket,
                         RideService rideService,
                         ReservationService reservationService,
                         UserService userService,
                         ObjectMapper mapper) {
        this.clientSocket = clientSocket;
        this.rideService = rideService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.mapper = mapper;
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
            case "LOGIN":
                handleLogin(request);
                break;

            case "REGISTER":
                handleRegister(request);
                break;

            default:
                handleAuthenticatedRequest(request);
        }
    }

    void handleLogin (Request request) throws IOException {
        LoginRequest loginRequest = mapper.convertValue(request.data(), LoginRequest.class);
        try{
            currentUser = userService.login(loginRequest);

            if (currentUser == null){
                sendResponse(new Response("ERROR", "Usuário ou senha incorretos.", null));
                return;
            }

            if (currentUser.userType() == UserType.DRIVER) {
                driverRequestHandler = new DriverRequestHandler(mapper, out, rideService, reservationService, currentUser);
            } else {
                passengerRequestHandler = new PassengerRequestHandler(mapper, out, rideService, reservationService, currentUser);
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

    void handleAuthenticatedRequest(Request request) throws IOException {
        if (currentUser == null) {
            sendResponse(new Response("ERROR", "Usuário não autenticado.", null));
            return;
        }

        if (currentUser.userType() == UserType.DRIVER) {
            driverRequestHandler.handle(request);
            return;
        }

        passengerRequestHandler.handle(request);
    }

}

