package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.util.PeerProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class PeerClient {
    private final PeerProperties peerProperties;
    private final RestClient restClient;

    public List<ItineraryResponse> searchItineraries(String origin,
                                                     String destination,
                                                     LocalDateTime departureTime,
                                                     Set<String> visitedServers){
        PeerItineraryRequest request = new PeerItineraryRequest(origin, destination, departureTime, visitedServers);

        ResponseEntity<List<ItineraryResponse>> response = restClient.post()
                .uri(peerProperties.peerServer() + "/api/itineraries/search")
                .body(request)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        // TODO: validar se recebeu realmente os itinerários ou recebeu 404.
        return response.getBody();
    }
}
