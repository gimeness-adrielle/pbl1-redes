package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.User;
import me.gimenez.server.reservations.ReservationRepository;
import me.gimenez.server.reservations.ReservationService;
import me.gimenez.server.rides.RideRepository;
import me.gimenez.server.transactions.TransactionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@Profile("test")
@RestController
@RequestMapping("/test/api")
@RequiredArgsConstructor
public class TestController {
    private final ReservationService service;
    private final RideRepository rideRepository;
    private final ReservationRepository reservationRepository;
    private final TransactionRepository transactionRepository;

    public record TestReservationRequest(
            List<Segment> segments,
            User user
    ) {}

    @PostMapping("/reservations")
    public ResponseEntity<Reservation> triggerCreateReservation(@RequestBody TestReservationRequest request) {
        Reservation reservation = service.createReservation(request.segments(), request.user());
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/data/reload")
    public ResponseEntity<Void> reload() {
        rideRepository.reload();
        reservationRepository.reload();
        transactionRepository.reload();

        return ResponseEntity.ok().build();
    }

    @GetMapping("/transactions")
    public Collection<Transaction> getAllTransactions() {
        return transactionRepository.getAllTransactions();
    }

}
