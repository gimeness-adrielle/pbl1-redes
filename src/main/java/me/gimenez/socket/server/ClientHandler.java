package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.requests.PublishRideRequest;
import me.gimenez.requests.Request;
import me.gimenez.services.RideService;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectMapper mapper;
    private final RideService rideService;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.rideService = new RideService();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run(){
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Client Connected");
            String json = in.readLine();
            Request request = mapper.readValue(json, Request.class);

            switch (request.type()){
                case "PUBLISH_RIDE":
                    PublishRideRequest publishRequest = mapper.convertValue(request.data(), PublishRideRequest.class);
                    rideService.publish(publishRequest);
                    break;

            }

            out.println("hello client!");

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

