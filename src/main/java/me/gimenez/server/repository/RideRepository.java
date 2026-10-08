package me.gimenez.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.Segment;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RideRepository {

    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "rides.json");
    private final Map<UUID, Ride> rides = new ConcurrentHashMap<>();
    private final Map<UUID, Segment> segments = new ConcurrentHashMap<>();

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
                    this.rides.put(ride.id(), ride);
                    for (Segment segment : ride.segments()) {
                        segments.put(segment.getId(), segment);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(Ride ride) throws IOException {
        rides.put(ride.id(), ride);
        for (Segment segment : ride.segments()) {
            segments.put(segment.getId(), segment);
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rides.values());
    }

    public void saveAll() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rides.values());
    }

    /** This function filters the rides data to retrieve all rides belonging
     *  to the driver specified by unique identifier in the parameter.
     * @param driverId the unique identifier of the driver wanting his rides.
     * @return the list containing all of that driver's rides.
     */
    public List<Ride> getRidesByDriver(UUID driverId){
        return rides.values().stream()
                .filter(ride -> ride.driverId().equals(driverId))
                .toList();
    }

    public List<Segment> findSegmentsById(List<UUID> ids) {
        if (!segments.keySet().containsAll(ids)) {
            return null;
        }

        return ids.stream()
                .map(segments::get)
                .toList();
    }

    public List<Segment> searchItinerary(LocalDate date){
        return segments.values().stream()
                .filter(segment -> segment.getDepartureAt().toLocalDate().equals(date)
                        && segment.getAvailableSeats() > 0)
                .toList();
    }

    public void releaseSeatsInSegments(List<UUID> segmentIds) throws IOException{
        for (UUID id : segmentIds){
            segments.computeIfPresent(id, (uuid, segment) -> {
                segment.setAvailableSeats(segment.getAvailableSeats() + 1);
                return segment;
            });
        }
        saveAll();
    }

    public Ride delete(UUID id) {
        Ride ride = rides.remove(id);
        for (Segment segment : ride.segments()) {
            segments.remove(segment.getId());
        }
        return ride;
    }

    public boolean hasSegmentsWithThatOrigin(String origin){
        return segments.values().stream().anyMatch(segment -> segment.getOrigin().equals(origin));
    }
}
