package me.gimenez.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.services.AuthService;
import me.gimenez.services.ReservationService;
import me.gimenez.services.RideService;
import org.springframework.stereotype.Component;

import java.net.Socket;

@RequiredArgsConstructor
@Component
public class HandlerFactory {
    private final ObjectMapper mapper;
    private final AuthService authService;
    private final RideService rideService;
    private final ReservationService reservationService;

    public ClientHandler createHandler(Socket clientSocket) {
        return new ClientHandler(
                clientSocket,
                mapper,
                authService,
                rideService,
                reservationService
        );
    }

}
