package me.gimenez.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Itinerary;
import me.gimenez.model.Ride;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RideRepository {

    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "rides.json");

    public RideRepository() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }
    public List<Ride> findAllRides() throws IOException {
        List<Ride> rides;
        if (Files.exists(path)) {
            rides = mapper.readValue(
                    path.toFile(),
                    new TypeReference<List<Ride>>() {}
            );
        } else {
            rides = new ArrayList<>();
        }

        return rides;
    }

    public void save(Ride ride) throws IOException {
        List<Ride> rides = findAllRides();

        rides.add(ride);

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rides);
    }
}
