package me.gimenez.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.dto.Request;
import me.gimenez.dto.Response;
import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.model.Ride;
import me.gimenez.model.users.User;
import me.gimenez.services.ReservationService;
import me.gimenez.services.RideService;

import java.io.IOException;
import java.io.PrintWriter;

public class DriverRequestHandler {
    private final PrintWriter out;
    private final ObjectMapper mapper;
    private final RideService rideService;
    private final ReservationService reservationService;

    private final User user;

    public DriverRequestHandler(ObjectMapper mapper,
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
            case "PUBLISH_RIDE":
                handlePublishRide(request);
                break;
        }
    }

    void sendResponse (Response response) throws IOException {
        String json = mapper.writeValueAsString(response);
        out.println(json);
    }

    void handlePublishRide(Request request) throws IOException {
        CreateRideRequest publishRequest = mapper.convertValue(request.data(), CreateRideRequest.class);
        Ride ride = rideService.publish(publishRequest, user);

        if (ride == null){
            sendResponse(new Response("ERROR", "Não foi possível publicar a carona.", null));
            return;
        }

        sendResponse(new Response("OK", "Carona publicada com sucesso!", ride));
    }


}
