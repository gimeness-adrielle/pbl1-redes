package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerReservationRequest;
import me.gimenez.domain.dto.responses.ReservationResponse;
import me.gimenez.domain.models.Segment;
import me.gimenez.server.services.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rides")
public class PeerController {
    private final RideService service;

    @GetMapping("/segments")
    public ResponseEntity<List<Segment>> getSegments(@RequestParam LocalDate date){
        List<Segment> responses = service.localSegments(date);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/reserve")
    public ReservationResponse reserve(@RequestBody PeerReservationRequest request){
        return null;
    }

}
