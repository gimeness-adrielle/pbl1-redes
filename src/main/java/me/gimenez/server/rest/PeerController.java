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
    private final LamportClock lamportClock = new LamportClock();

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
        lamportClock.update(request.logicalTimestamp());

        TransactionStatus status = transactionService
                .prepare(txId, request.segmentIds(), request.coordinatorId(), null, request.transactionType());

        TransactionDTO.Response response = new TransactionDTO.Response(txId, status, peerProperties.selfId(), lamportClock.tick());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{txId}/commit")
    public ResponseEntity<TransactionDTO.Response> commit (@PathVariable UUID txId, TransactionDTO.Request request){
        lamportClock.update(request.logicalTimestamp());
        Transaction transaction = transactionService.commit(txId,  request.transactionType());

        TransactionDTO.Response response = new TransactionDTO.Response(txId, transaction.getCurrentStatus(), peerProperties.selfId(), 0);

        if (transaction.getCurrentStatus() != TransactionStatus.COMMITTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/{txId}/abort")
    public ResponseEntity<TransactionDTO.Response> abort (@PathVariable UUID txId, @RequestBody long timestamp){
        lamportClock.update(timestamp);
        TransactionStatus status = transactionService.abort(txId);

        TransactionDTO.Response response = new TransactionDTO.Response(txId, status, peerProperties.selfId(), 0);

        if (status != TransactionStatus.ABORTED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{txId}/status")
    public TransactionDecision getDecisionStatus(@PathVariable UUID txId){
        return coordinatorService.getTxDecision(txId);
    }

    @PostMapping("/reservations")
    public List<ReservationDTO.Response> getReservations(@RequestBody List<UUID> segmentsIds){
        List<Reservation> reservations = reservationService.getReservationsBySegments(segmentsIds);

        return reservations.stream()
                .map(reservation -> new ReservationDTO.Response(
                        reservation.id(),
                        reservation.segments().stream().map(SegmentDTO.Response::from).toList()
                ))
                .toList();
    }

}
