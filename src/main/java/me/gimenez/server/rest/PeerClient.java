package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.models.Segment;
import me.gimenez.util.PeerProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
@Component
public class PeerClient {
    private final PeerProperties peerProperties;
    private final RestClient restClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public List<Segment> fetchSegments(LocalDate date){
        List<CompletableFuture<List<Segment>>> futures = peerProperties.servers().stream()
                .map(peer -> CompletableFuture
                        .supplyAsync(() -> fetchFrom(peer, date), executor)
                        .exceptionally(e -> List.of())
                ).toList();

        return futures.stream().map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    private List<Segment> fetchFrom(String peer, LocalDate date){
        List<Segment> body = restClient.get()
                .uri(peer + "/api/rides/segments?date={date}", date)
                .retrieve()
                .body(new ParameterizedTypeReference<>(){});
        log.info("Peer {} returned {} segments",
                peer, body == null ? 0 : body.size());
        return body == null ? List.of() : body;
    }
}
