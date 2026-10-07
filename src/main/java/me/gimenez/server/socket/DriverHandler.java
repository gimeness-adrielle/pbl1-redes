package me.gimenez.server.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.gimenez.domain.dto.requests.Request;
import me.gimenez.domain.dto.requests.RideRequest;
import me.gimenez.domain.dto.responses.Response;
import me.gimenez.domain.dto.responses.RideResponse;
import me.gimenez.server.exceptions.NotFoundException;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.User;
import me.gimenez.server.services.RideService;

import java.util.List;
import java.util.UUID;

public class DriverHandler {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final RideService rideService;

    private final User user;

    public DriverHandler(ObjectMapper mapper,
                         RideService rideService,
                         ClientHandler clientHandler,
                         User user) {
        this.mapper = mapper;
        this.rideService = rideService;
        this.clientHandler = clientHandler;
        this.user = user;
    }

    public void handle(Request request) {
        switch (request.type()){
            case "PUBLISH_RIDE":
                handlePublishRide(request);
                break;

            case "LIST_RIDES":
                handleListRides();
                break;

            case "DELETE_RIDE":
                handleDeleteRide(request);
                break;
        }
    }

    void handlePublishRide(Request request) {
        try {
            RideRequest rideRequest = mapper.convertValue(request.data(), RideRequest.class);
            rideService.create(rideRequest, user);

            clientHandler.sendResponse(Response.ok("Viagem publicada com sucesso!", null));
        } catch (PersistenceErrorException e) {
            clientHandler.sendResponse(Response.error(e.getMessage()));
        }
    }

    void handleListRides() {
        List<Ride> rides = rideService.getRidesByDriver(user.id());
        List<RideResponse> responses = rides.stream().map(RideResponse::from).toList();

        clientHandler.sendResponse(Response.ok("Viagens buscadas com sucesso!", responses));
    }

    void handleDeleteRide(Request request) {
        try {
            UUID rideId = mapper.convertValue(request.data(), UUID.class);
            rideService.deleteRide(rideId);

            clientHandler.sendResponse(Response.ok("Viagem deletada com sucesso!", null));
        } catch (NotFoundException e){
            clientHandler.sendResponse(Response.notFound(e.getMessage()));
        } catch (PersistenceErrorException e){
            clientHandler.sendResponse(Response.error(e.getMessage()));
        }
    }

}
