package me.gimenez.socket.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.users.User;
import me.gimenez.requests.PublishRideRequest;
import me.gimenez.requests.Request;
import me.gimenez.requests.SearchRideRequest;
import me.gimenez.requests.auth.LoginRequest;
import me.gimenez.requests.auth.RegisterRequest;

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

    public void sendRequest(String type, Object data) throws JsonProcessingException {
        Request request = new Request(type, data);

        String json = mapper.writeValueAsString(request);

        System.out.println(json);
        out.println(json);
    }

    public User login (LoginRequest request) throws IOException {
        sendRequest("LOGIN", request);

        String response = in.readLine();

        if (response.equals("LOGIN_ERROR")) {
            return null;
        }

        return mapper.readValue(response, User.class);
    }

    public List<Itinerary> searchRides(SearchRideRequest request) throws IOException {
        sendRequest("SEARCH_RIDES", request);

        String response = in.readLine();

        return mapper.readValue(response, new TypeReference<>() {});
    }

}
