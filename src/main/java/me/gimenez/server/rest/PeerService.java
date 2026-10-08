package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.domain.dto.requests.PeerReservationRequest;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Reservation;
import me.gimenez.server.repository.RideRepository;
import me.gimenez.server.services.RideService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PeerService {
    private final RideRepository repository;
    private final RideService rideService;

    public List<ItineraryResponse> search (PeerItineraryRequest request){
        if (!repository.hasSegmentsWithThatOrigin(request.origin())){
            return List.of();
        }

        return rideService.searchItinerary(request);
    }

    public Reservation reserve (PeerReservationRequest request){
        return null;
    }

}
