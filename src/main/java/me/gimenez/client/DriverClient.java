package me.gimenez.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.dto.responses.Response;
import me.gimenez.dto.requests.RideRequest;
import me.gimenez.dto.responses.RideResponse;
import me.gimenez.dto.requests.SegmentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DriverClient {
    private final Client client;
    private final ObjectMapper mapper;

    public String publishRide(int seats, List<SegmentRequest> segments) {
        RideRequest request = new RideRequest(segments, seats);
        return client.sendRequest("PUBLISH_RIDE", request).message();
    }

    public SegmentRequest createSegmentRequest(
            String origin,
            String destination,
            double price,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ){
        return new SegmentRequest(origin, destination, price, departureAt, arrivalAt);
    }

    public List<RideResponse> getRidesByDriver(){
        Response response = client.sendRequest("LIST_RIDES", null);
        return mapper.convertValue(response.data(), new TypeReference<>() {});
    }

    public String deleteRide(UUID rideId){
        Response response = client.sendRequest("DELETE_RIDE", rideId);
        return response.message();
    }
}
