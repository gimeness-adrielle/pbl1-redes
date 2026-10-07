package me.gimenez.server.rest;

import me.gimenez.domain.dto.requests.PeerReservationRequest;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.domain.dto.responses.ReservationResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
public class PeerController {

    @PostMapping("/search")
    public List<ItineraryResponse> search(@RequestBody PeerItineraryRequest request){
        return null;
    }

    @PostMapping("/reserve")
    public ReservationResponse reserve(@RequestBody PeerReservationRequest request){
        return null;
    }

}
