package me.gimenez.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.PublishRideRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RideService {

    private final RideRepository repository;

    public RideService() {
        this.repository = new RideRepository();
    }

    public void publish(PublishRideRequest request){
        System.out.println("Criando a ride...");
        List<Segment> segments = new ArrayList<>();

        for (int i = 0; i < request.routes().size() - 1; i++) {
            Segment segment = new Segment(
                request.routes().get(i),
                    request.routes().get(i+1),
                    request.prices().get(i),
                    request.seats()
            );

            segments.add(segment);
        }

        Ride ride = new Ride(
                UUID.randomUUID(),
                request.routes(),
                request.date(),
                request.departureTime(),
                segments
        );

        try {
            System.out.println("Entrou pra salvar");
            repository.save(ride);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
