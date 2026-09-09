package me.gimenez.socket.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.requests.PublishRideRequest;
import me.gimenez.requests.Request;

import java.io.*;
import java.net.Socket;

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

    public String sendMessage(String message) throws IOException {
        out.println(message);
        return in.readLine();
    }

    public void publishRide(PublishRideRequest request) throws JsonProcessingException {
        Request message = new Request("PUBLISH_RIDE", request);

        String json = mapper.writeValueAsString(message);

        System.out.println(json);
        out.println(json);
    }

}
