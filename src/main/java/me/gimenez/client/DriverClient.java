package me.gimenez.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.RideDTO;
import me.gimenez.domain.dto.SegmentDTO;
import me.gimenez.domain.dto.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DriverClient {
    private final Client client;
    private final ObjectMapper mapper;

    public String publishRide(int seats, List<SegmentDTO.Request> segments) {
        RideDTO.Request request = new RideDTO.Request(segments, seats);
        return client.sendRequest("PUBLISH_RIDE", request).message();
    }

    public SegmentDTO.Request createSegmentRequest(
            String origin,
            String destination,
            double price,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ){
        return new SegmentDTO.Request(origin, destination, price, departureAt, arrivalAt);
    }

    public List<RideDTO.Response> getRidesByDriver(){
        Response response = client.sendRequest("LIST_RIDES", null);
        return mapper.convertValue(response.data(), new TypeReference<>() {});
    }

    public String deleteRide(UUID rideId){
        Response response = client.sendRequest("DELETE_RIDE", rideId);
        return response.message();
    }
}
