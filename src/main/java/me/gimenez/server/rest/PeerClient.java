package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.TransactionDTO;
import me.gimenez.domain.models.*;
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

    public List<Reservation> fetchReservations (List<UUID> segmentIds){
        List<CompletableFuture<List<ReservationDTO.Response>>> futures = peerProperties.peerUrls().stream()
                .map(peer -> CompletableFuture
                        .supplyAsync(() -> fetchReservationsFrom(peer, segmentIds), executor)
                        .exceptionally(e -> {
                            log.error("[ERROR] cannot found reservations from {}", peer);
                            return List.of();
                        })
                ).toList();

        List<ReservationDTO.Response> reservations = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return reservations.stream()
                .map(response -> new Reservation(
                        response.id(),
                        null,
                        response.segments().stream()
                                .map(Segment::from)
                                .toList()))
                .toList();
    }

    private List<ReservationDTO.Response> fetchReservationsFrom(String peer, List<UUID> segmentIds){
        List<ReservationDTO.Response> response = restClient.post()
                .uri(peer + "/api/reservations")
                .body(segmentIds)
                .retrieve()
                .body(new ParameterizedTypeReference<>(){});

        return response == null ? List.of() : response;
    }

    public List<Segment> fetchSegments(LocalDate date){
        List<CompletableFuture<List<Segment>>> futures = peerProperties.peerUrls().stream()
                .map(peer -> CompletableFuture
                        .supplyAsync(() -> fetchFrom(peer, date), executor)
                        .exceptionally(e -> {
                            log.error("[ERROR] cannot found segments from {} from date {}", peer, date);
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

    public List<TransactionDTO.Response> sendPrepare (UUID txId, Map<String, List<UUID>> segmentsByServer, TransactionType transactionType, long timestamp){
        Map<String, String> peerTable = segmentsByServer.keySet().stream()
                .filter(serverId -> !serverId.equals(peerProperties.selfId()))
                .collect(Collectors.toMap(
                        serverId -> serverId,
                        peerProperties::getPeerUrl
                ));

        List<CompletableFuture<TransactionDTO.Response>> futures = peerTable.entrySet().stream()
                .map(entry -> CompletableFuture
                        .supplyAsync(() -> prepareFrom(txId, entry.getValue(), segmentsByServer.get(entry.getKey()), transactionType, timestamp), executor)
                        .exceptionally(e -> {
                            log.warn("[ERROR]: Cannot prepare transaction for {} from {}", entry.getKey(), txId);
                            return null;
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public List<TransactionDTO.Response> sendDecision(UUID txId, List<String> serversToSend, String decision){
        List<String> peerUrls = serversToSend.stream()
                .filter(serverId -> !serverId.equals(peerProperties.selfId()))
                .map(peerProperties::getPeerUrl)
                .toList();

        List<CompletableFuture<TransactionDTO.Response>> futures = peerUrls.stream()
                .map(peerUrl -> CompletableFuture
                        .supplyAsync(() -> sendDecisionTo(txId, peerUrl, decision), executor)
                        .exceptionally(e -> {
                            log.warn("[ERROR]: Cannot {} transaction for {} from {}", decision, peerUrl, txId);
                            return null;
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private TransactionDTO.Response sendDecisionTo(UUID txId, String peerUrl, String decision) {
        log.debug("Sending {} to {}", decision, peerUrl);
        return restClient.post()
                .uri(peerUrl + "/api/transactions/{txId}/{decision}", txId, decision)
                .retrieve()
                .body(TransactionDTO.Response.class);
    }

    private TransactionDTO.Response prepareFrom(UUID txId, String peerUrl, List<UUID> segmentIds, TransactionType transactionType, long timestamp) {
        TransactionDTO.Request request = new TransactionDTO.Request(txId, peerProperties.selfId(), segmentIds, transactionType, timestamp);

        log.debug("Sending prepare to {}", peerUrl);
        return restClient.post()
                .uri(peerUrl + "/api/transactions/{txId}/prepare", txId)
                .body(request)
                .retrieve()
                .body(TransactionDTO.Response.class);
    }

    public TransactionDTO.HealthResponse askStatus(String coordinatorId, UUID txId) {
        log.debug("Asking {} the status from transaction {}", coordinatorId, txId);
        return restClient.get()
                .uri(peerProperties.getPeerUrl(coordinatorId) + "/api/transactions/{txId}/status", txId)
                .retrieve()
                .body(TransactionDTO.HealthResponse.class);
    }
}
