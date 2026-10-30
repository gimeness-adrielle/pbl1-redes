package me.gimenez.server.communication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.requests.TransactionRequest;
import me.gimenez.domain.dto.responses.TransactionResponse;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.TransactionDecision;
import me.gimenez.util.PeerProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class PeerClient {
    private final PeerProperties peerProperties;
    private final RestClient restClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public List<Segment> fetchSegments(LocalDate date){
        List<CompletableFuture<List<Segment>>> futures = peerProperties.peerUrls().stream()
                .map(peer -> CompletableFuture
                        .supplyAsync(() -> fetchFrom(peer, date), executor)
                        .exceptionally(e -> {
                            log.error("[ERROR] cannot found segments from {} from date {}", peer, date, e);
                            return List.of();
                        })
                ).toList();

        List<Segment> segments = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        log.info("Remote segments found: {}", segments.size());

        return segments;
    }

    private List<Segment> fetchFrom(String peer, LocalDate date){
        List<Segment> response = restClient.get()
                .uri(peer + "/api/rides/segments?date={date}", date)
                .retrieve()
                .body(new ParameterizedTypeReference<>(){});

        return response == null ? List.of() : response;
    }

    // TWO-PHASE-COMMIT METHODS

    public List<TransactionResponse> sendPrepare (UUID txId, Map<String, List<UUID>> segmentsByServer){
        Map<String, String> peerTable = segmentsByServer.keySet().stream()
                .filter(serverId -> !serverId.equals(peerProperties.selfId()))
                .collect(Collectors.toMap(
                        serverId -> serverId,
                        peerProperties::getPeerUrl
                ));

        List<CompletableFuture<TransactionResponse>> futures = peerTable.entrySet().stream()
                .map(entry -> CompletableFuture
                        .supplyAsync(() -> prepareFrom(txId, entry.getValue(), segmentsByServer.get(entry.getKey())))
                        .exceptionally(e -> {
                            log.warn("[ERROR]: Cannot prepare transaction for {} from {}", entry.getKey(), txId);
                            return null;
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private TransactionResponse prepareFrom(UUID txId, String peerUrl, List<UUID> segmentIds) {
        TransactionRequest request = new TransactionRequest(
                txId,
                peerProperties.selfId(),
                0,
                segmentIds
        );

        log.debug("Sending prepare to {}", peerUrl);
        return restClient.post()
                .uri(peerUrl + "/api/transactions/{txId}/prepare", txId)
                .body(request)
                .retrieve()
                .body(TransactionResponse.class);
    }

    public List<TransactionResponse> sendDecision(UUID txId, String decision, List<String> serversToSend){
        List<String> peerUrls = serversToSend.stream()
                .filter(serverId -> !serverId.equals(peerProperties.selfId()))
                .map(peerProperties::getPeerUrl)
                .toList();

        List<CompletableFuture<TransactionResponse>> futures = peerUrls.stream()
                .map(peerUrl -> CompletableFuture
                        .supplyAsync(() -> decisionFrom(txId, decision, peerUrl), executor)
                        .exceptionally(e -> {
                            log.warn("[ERROR]: Cannot commit/abort transaction for {} from {}", peerUrl, txId);
                            return null;
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private TransactionResponse decisionFrom(UUID txId, String decision, String peerUrl) {
        log.debug("Sending decision {} to {}", decision, peerUrl);
        return restClient.post()
                .uri(peerUrl + "/api/transactions/{txId}/{decision}", txId, decision)
                .retrieve()
                .body(TransactionResponse.class);
    }

    public TransactionDecision askStatus(String coordinatorId, UUID txId) {
        log.debug("Asking {} the status from transaction {}", coordinatorId, txId);
        return restClient.get()
                .uri(peerProperties.getPeerUrl(coordinatorId) + "/api/transactions/{txId}/status", txId)
                .retrieve()
                .body(TransactionDecision.class);
    }
}
