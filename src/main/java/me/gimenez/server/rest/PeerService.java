package me.gimenez.server.rest;

import lombok.RequiredArgsConstructor;
import me.gimenez.domain.dto.requests.PeerItineraryRequest;
import me.gimenez.domain.dto.requests.PeerReservationRequest;
import me.gimenez.domain.dto.responses.ItineraryResponse;
import me.gimenez.domain.models.Reservation;
import me.gimenez.server.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PeerService {
    private final RideRepository repository;

    public List<ItineraryResponse> search (PeerItineraryRequest request){
        return null;
    }

    public Reservation reserve (PeerReservationRequest request){
        return null;
    }

}
