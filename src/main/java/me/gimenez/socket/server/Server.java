package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.repository.ReservationRepository;
import me.gimenez.repository.RideRepository;
import me.gimenez.services.ReservationService;
import me.gimenez.services.RideService;
import me.gimenez.services.UserService;

import java.io.*;
import java.net.*;

public class Server {
    private final RideService rideService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final ObjectMapper mapper;

    public Server(){
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        RideRepository rideRepository = new RideRepository(mapper);
        ReservationRepository reservationRepository = new ReservationRepository(mapper);

        this.rideService = new RideService(rideRepository, reservationRepository);
        this.reservationService = new ReservationService(reservationRepository, rideRepository);
        this.userService = new UserService();
    }

    public void start () throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);

        System.out.println("Server started on port 5000");

        while (true) {
            Socket clientSocket = serverSocket.accept();

            new Thread(new ClientHandler(
                    clientSocket,
                    rideService,
                    reservationService,
                    userService,
                    mapper
            )).start();
        }

    }
}
