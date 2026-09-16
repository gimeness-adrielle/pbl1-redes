package me.gimenez.socket.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.dto.reservation.CreateReservationRequest;
import me.gimenez.dto.reservation.DeleteReservationRequest;
import me.gimenez.dto.ride.CreateRideRequest;
import me.gimenez.dto.ride.DeleteRideRequest;
import me.gimenez.dto.ride.SearchRideRequest;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Reservation;
import me.gimenez.model.Ride;
import me.gimenez.model.users.User;
import me.gimenez.dto.*;
import me.gimenez.dto.auth.LoginRequest;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Client {
    private final PrintWriter out;
    private final BufferedReader in;

    private final ObjectMapper mapper;

    public Client() throws IOException {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String host = System.getenv().getOrDefault("SERVER_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "5000"));

        Socket socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Response sendRequest(String type, Object data) throws IOException {
        Request request = new Request(type, data);

        String json = mapper.writeValueAsString(request);

        System.out.println("VEIO DO USUÁRIO: " + json);
        out.println(json);

        String response = in.readLine();

        return mapper.readValue(response, Response.class);
    }

    public Response login (LoginRequest request) {
        Response response = null;
        try {
            response = sendRequest("LOGIN", request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        User user = mapper.convertValue(response.data(), User.class);

        return new Response(response.status(), response.message(), user);
    }

    // DRIVER
    public Response publishRide(CreateRideRequest request){
        try{
            return sendRequest("PUBLISH_RIDE", request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ride> listRides(){
        try{
            Response response = sendRequest("LIST_RIDES", null);
            System.out.println(response);
            return mapper.convertValue(response.data(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response deleteRide(DeleteRideRequest request){
        try{
            return sendRequest("DELETE_RIDE", request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // PASSENGER
    public List<Itinerary> searchRides(SearchRideRequest request) {
        Response response = null;
        try {
            response = sendRequest("SEARCH_RIDES", request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mapper.convertValue(response.data(), new TypeReference<>() {});
    }

    public Response reserveItinerary (CreateReservationRequest request){
        try {
            return sendRequest("RESERVE_ITINERARY", request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Reservation> listReservations (){
        try {
            Response response = sendRequest("LIST_RESERVATIONS", null);

            return mapper.convertValue(response.data(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Response deleteReservation (DeleteReservationRequest request){
        try {
            return sendRequest("DELETE_RESERVATION", request);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
