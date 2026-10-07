package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.util.PeerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class PeerClient {
    private final PeerProperties peerProperties;
    private final RestClient restClient;

    @Value("${server.id}")
    private String serverId;

    public void searchItineraries(String origin, String destination, LocalDateTime departureTime){
        List<String> visitedServers = List.of(serverId);
        PeerItineraryRequest request = new PeerItineraryRequest(origin, destination, departureTime, visitedServers);

        // TODO: fazer o post aqui com restClient.
    }
}
