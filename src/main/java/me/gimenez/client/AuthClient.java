package me.gimenez.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.dto.responses.Response;
import me.gimenez.dto.requests.LoginRequest;
import me.gimenez.dto.requests.RegisterRequest;
import me.gimenez.model.UserType;

@RequiredArgsConstructor
public class AuthClient {
    private final Client client;
    private final ObjectMapper mapper;

    public Response login (String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        Response response = client.sendRequest("LOGIN", request);

        UserType currentUserType = mapper.convertValue(response.data(), UserType.class);

        return new Response(response.status(), response.message(), currentUserType);
    }

    public Response register (String email, String name, String password, UserType userType){
        RegisterRequest request = new RegisterRequest(email, name, password, userType);
        return client.sendRequest("REGISTER", request);
    }

}
