package me.gimenez.server.communication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.requests.TransactionRequest;
import me.gimenez.domain.dto.responses.TransactionResponse;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionDecision;
import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.server.rides.RideService;
import me.gimenez.server.transactions.TransactionCoordinatorService;
import me.gimenez.server.transactions.TransactionService;
import me.gimenez.util.PeerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class PeerController {
    private final PeerProperties peerProperties;
    private final RideService rideService;
    private final TransactionService transactionService;
    private final TransactionCoordinatorService coordinatorService;

    @GetMapping("/rides/segments")
    public ResponseEntity<List<Segment>> getSegments(@RequestParam LocalDate date){
        List<Segment> responses = rideService.localSegments(date);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/transactions/{txId}/prepare")
    public ResponseEntity<TransactionResponse> prepare(@PathVariable UUID txId, @RequestBody TransactionRequest request) {
        TransactionStatus status = transactionService.prepare(txId, request.segmentIds(), request.coordinatorId(), null);

        TransactionResponse response = new TransactionResponse(txId, status, peerProperties.selfId(), 0);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{txId}/commit")
    public ResponseEntity<TransactionResponse> commit (@PathVariable UUID txId){
        Transaction transaction = transactionService.commit(txId);

        TransactionResponse response = new TransactionResponse(txId, transaction.getCurrentStatus(), peerProperties.selfId(), 0);

        if (transaction.getCurrentStatus() != TransactionStatus.COMMITTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{txId}/abort")
    public ResponseEntity<TransactionResponse> abort (@PathVariable UUID txId){
        TransactionStatus status = transactionService.abort(txId);

        TransactionResponse response = new TransactionResponse(txId, status, peerProperties.selfId(), 0);

        if (status != TransactionStatus.ABORTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{txId}/status")
    public TransactionDecision getDecisionStatus(@PathVariable UUID txId){
        return coordinatorService.getTxDecision(txId);
    }

}
