package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.SegmentDTO;
import me.gimenez.domain.dto.TransactionDTO;
import me.gimenez.domain.models.*;
import me.gimenez.server.reservations.ReservationService;
import me.gimenez.server.rides.RideService;
import me.gimenez.server.transactions.TransactionCoordinatorService;
import me.gimenez.server.transactions.TransactionService;
import me.gimenez.util.LamportClock;
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
    private final ReservationService reservationService;

    @GetMapping("/rides/segments")
    public ResponseEntity<List<Segment>> getSegments(@RequestParam LocalDate date){
        List<Segment> responses = rideService.localSegments(date);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/transactions/{txId}/prepare")
    public ResponseEntity<TransactionDTO.Response> prepare(@PathVariable UUID txId, @RequestBody TransactionDTO.Request request) {
        Transaction tx = TransactionDTO.Request.from(request);
        TransactionStatus status = transactionService.prepare(tx);

        return ResponseEntity.ok(new TransactionDTO.Response(txId, status, peerProperties.selfId()));
    }

    @PostMapping("/transactions/{txId}/commit")
    public ResponseEntity<TransactionDTO.Response> commit (@PathVariable UUID txId){
        Transaction transaction = transactionService.commit(txId);

        TransactionDTO.Response response = new TransactionDTO.Response(txId, transaction.getCurrentStatus(), peerProperties.selfId());

        if (transaction.getCurrentStatus() != TransactionStatus.COMMITTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{txId}/abort")
    public ResponseEntity<TransactionDTO.Response> abort (@PathVariable UUID txId){
        TransactionStatus status = transactionService.abort(txId);

        TransactionDTO.Response response = new TransactionDTO.Response(txId, status, peerProperties.selfId());

        if (status != TransactionStatus.ABORTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{txId}/status")
    public TransactionDTO.HealthResponse checkTransaction(@PathVariable UUID txId){
        Transaction transaction = coordinatorService.getTx(txId);

        return new TransactionDTO.HealthResponse(
                transaction.getTxId(),
                transaction.getDecision()
        );
    }

    @PostMapping("/reservations")
    public ResponseEntity<List<ReservationDTO.Response>> getReservations(@RequestBody List<UUID> segmentsIds){
        List<Reservation> reservations = reservationService.getReservationsBySegments(segmentsIds);

        List<ReservationDTO.Response> responses = reservations.stream()
                .map(reservation -> new ReservationDTO.Response(
                        reservation.id(),
                        reservation.segments().stream().map(SegmentDTO.Response::from).toList()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

}
