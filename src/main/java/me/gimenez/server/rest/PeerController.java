package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerReservationRequest;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.domain.dto.responses.ReservationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/itineraries")
public class PeerController {
    private final PeerService service;

    @PostMapping("/search")
    public ResponseEntity<List<ItineraryResponse>> search(@RequestBody PeerItineraryRequest request){
        List<ItineraryResponse> responses = service.search(request);
        if (responses == null){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/reserve")
    public ReservationResponse reserve(@RequestBody PeerReservationRequest request){
        return null;
    }

}
