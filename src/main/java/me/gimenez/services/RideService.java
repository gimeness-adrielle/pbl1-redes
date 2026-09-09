package me.gimenez.services;

import me.gimenez.model.Ride;
import me.gimenez.model.Segment;
import me.gimenez.model.users.User;
import me.gimenez.repository.RideRepository;
import me.gimenez.requests.PublishRideRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RideService {

    private final RideRepository repository;

    public RideService() {
        this.repository = new RideRepository();
    }

    public void publish(PublishRideRequest request, User driver){
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
                driver.id(),
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
