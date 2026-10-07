package me.gimenez.server.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.Request;
import me.gimenez.domain.dto.responses.Response;
import me.gimenez.server.exceptions.CommunicationErrorException;
import me.gimenez.server.exceptions.InvalidUserException;
import me.gimenez.domain.models.User;
import me.gimenez.domain.dto.requests.LoginRequest;
import me.gimenez.domain.dto.requests.RegisterRequest;
import me.gimenez.domain.models.UserType;
import me.gimenez.server.services.ReservationService;
import me.gimenez.server.services.RideService;
import me.gimenez.server.services.AuthService;

import java.io.*;
import java.net.Socket;

@RequiredArgsConstructor
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private PrintWriter out;
    private final ObjectMapper mapper;

    private final AuthService authService;
    private final RideService rideService;
    private final ReservationService reservationService;

    private DriverHandler driverHandler;
    private PassengerHandler passengerHandler;

    private User currentUser;

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
            throw new RuntimeException(e);
        } finally{
            System.out.println("Client disconnected.");
        }
    }

    void sendResponse (Response response) {
        try {
            String json = mapper.writeValueAsString(response);
            out.println(json);
        } catch (JsonProcessingException e) {
            throw new CommunicationErrorException("Erro na comunicação de rede com o cliente." + e);
        }
    }

    void handleRequest(Request request){
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

    void handleLogin (Request request) {
        LoginRequest loginRequest = mapper.convertValue(request.data(), LoginRequest.class);

        try {
            currentUser = authService.login(loginRequest);
        } catch (InvalidUserException e){
            sendResponse(Response.error(e.getMessage()));
            return;
        }

        if (currentUser.userType() == UserType.DRIVER) {
            driverHandler = new DriverHandler(mapper, rideService, this, currentUser);
        } else {
            passengerHandler = new PassengerHandler(mapper, this, rideService, reservationService, currentUser);
        }

        sendResponse(Response.ok("Login realizado com sucesso!", currentUser.userType()));
    }

    void handleRegister (Request request)  {
        RegisterRequest registerRequest = mapper.convertValue(request.data(), RegisterRequest.class);
        authService.register(registerRequest);
        sendResponse(new Response("CREATED", "Nova conta registrada com sucesso.", null));
    }

    void handleAuthenticatedRequest(Request request) {
        if (currentUser == null) {
            sendResponse(new Response("ERROR", "Usuário não autenticado.", null));
            return;
        }

        if (currentUser.userType() == UserType.DRIVER) {
            driverHandler.handle(request);
            return;
        }

        passengerHandler.handle(request);
    }

}

