package me.gimenez.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Ride;
import me.gimenez.model.Segment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RideRepository {

    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "rides.json");
    private final Map<UUID, Ride> rides = new HashMap<>();
    private final Map<UUID, Segment> segments = new HashMap<>();

    public RideRepository(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.registerModule(new JavaTimeModule());
        loadData();
    }

    public void loadData(){
        try {
            if (Files.exists(path)) {
                List<Ride> rides = mapper.readValue(path.toFile(), new TypeReference<List<Ride>>() {});

                for (Ride ride : rides) {
                    this.rides.put(ride.getId(), ride);
                    for (Segment segment : ride.getSegments()) {
                        segments.put(segment.getId(), segment);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<Ride> findAllRides() throws IOException {
        return new ArrayList<>(rides.values());
    }

    public void save(Ride ride) throws IOException {
        rides.put(ride.getId(), ride);
        for (Segment segment : ride.getSegments()) {
            segments.put(segment.getId(), segment);
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rides.values());
    }

    public Segment findSegmentById(UUID id) throws IOException {
        return segments.get(id);
    }

    public void saveAll() throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), rides.values());
    }
}
