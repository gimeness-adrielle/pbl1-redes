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

    /** Loads the ride's data JSON into a {@link ConcurrentHashMap}, where the key is the object's
     * unique identifier and the value is the object itself. This is necessary to speed up queries.
     */
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

    /** This function is responsible for saving a specified new Ride. It saves data both the
     * in memory {@link ConcurrentHashMap} and JSON format.
     * @param ride the new ride to be persisted on data.
     * @throws IOException thrown if the Jackson library fails to write to the JSON file.
     */
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

    /** This function filters the rides data to retrieve all rides belonging to the
     * driver specified by unique identifier in the parameter.
     * @param driverId the unique identifier of the driver wanting his rides.
     * @return the list containing all of that driver's rides.
     */
    public List<Ride> getRidesByDriver(UUID driverId){
        return rides.values().stream()
                .filter(ride -> ride.driverId().equals(driverId))
                .toList();
    }

    /** This function retrieves a set of segments specified by their unique identifier.
     * It is an atomic operation: it returns null if at least one of the segment is not found.
     * @param ids the segments to be retrieved.
     * @return the segments found or null.
     */
    public List<Segment> findSegmentsById(List<UUID> ids) {
        if (!segments.keySet().containsAll(ids)) {
            return null;
        }

        return ids.stream()
                .map(segments::get)
                .toList();
    }

    /** Filters segments by the specified date and whether they have more than zero available
     * seats.
     * @param date the date to filter by.
     * @return the filtered segments.
     */
    public List<Segment> searchItinerary(LocalDate date){
        return segments.values().stream()
                .filter(segment -> (segment.getDepartureAt().toLocalDate().isEqual(date)
                        || segment.getDepartureAt().toLocalDate().isAfter(date))
                        && segment.getAvailableSeats() > 0)
                .toList();
    }

    /** This operation cancels seat reservations on segments. It increases the number of
     * seats available on each specified segments.
     * @param segmentIds the segments for which reservations are to be canceled.
     * @throws IOException if the Jackson library fails to write to the JSON file.
     */
    public void releaseSeatsInSegments(List<UUID> segmentIds) throws IOException{
        for (UUID id : segmentIds){
            segments.computeIfPresent(id, (uuid, segment) -> {
                segment.setAvailableSeats(segment.getAvailableSeats() + 1);
                return segment;
            });
        }
        saveAll();
    }

    /** Deletes a ride, and its respective segments from the in-memory data.
     * @param id the unique identifier of the ride to be deleted.
     * @return the deleted ride or null if the ride is not found.
     */
    public Ride delete(UUID id) {
        Ride ride = rides.remove(id);
        for (Segment segment : ride.segments()) {
            segments.remove(segment.getId());
        }
        return ride;
    }

    /** Checks if there are persisted segments with the origin specified in the parameter.
     * @param origin the desired origin to check.
     * @return True if there is a match between the segment's origin and the desired origin.
     * Otherwise, returns False.
     */
    public boolean hasSegmentsWithThatOrigin(String origin){
        return segments.values().stream().anyMatch(segment -> segment.getOrigin().equals(origin));
    }
}
