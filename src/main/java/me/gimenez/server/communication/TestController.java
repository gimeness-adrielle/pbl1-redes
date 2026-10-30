package me.gimenez.server.communication;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.models.Reservation;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.server.reservations.ReservationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("test")
@RestController
@RequestMapping("/test/api")
@RequiredArgsConstructor
public class TestController {
    private final ReservationService service;

    public record TestReservationRequest(
            List<Segment> segments,
            User user
    ) {}

    @PostMapping("/reservations")
    public ResponseEntity<Reservation> triggerCreateReservation(@RequestBody TestReservationRequest request) {
        Reservation reservation = service.createReservation(request.segments(), request.user());
        return ResponseEntity.ok(reservation);
    }

}
