package me.gimenez.socket.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.users.User;
import me.gimenez.requests.*;
import me.gimenez.requests.auth.LoginRequest;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Client {
    private final PrintWriter out;
    private final BufferedReader in;

    private final ObjectMapper mapper;

    public Client(String ip, int port) throws IOException {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Socket socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public Response sendRequest(String type, Object data) throws IOException {
        Request request = new Request(type, data);

        String json = mapper.writeValueAsString(request);

        System.out.println(json);
        out.println(json);

        String response = in.readLine();

        return mapper.readValue(response, Response.class);
    }

    public User login (LoginRequest request) throws IOException {
        Response response = sendRequest("LOGIN", request);

        if (response.status().equals("400")) {
            return null;
        }

        return mapper.convertValue(response.data(), User.class);
    }

    public List<Itinerary> searchRides(SearchRideRequest request) throws IOException {
        Response response = sendRequest("SEARCH_RIDES", request);

        return mapper.convertValue(response, new TypeReference<>() {});
    }

    public Response reserveItinerary (ReserveItineraryRequest request) throws IOException {
        return sendRequest("RESERVE_ITINERARY", request);
    }

}
