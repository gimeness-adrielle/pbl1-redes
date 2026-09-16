package me.gimenez.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.model.Reservation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReservationRepository {
    private final ObjectMapper mapper;
    private final Path path = Paths.get("data", "reservations.json");
    private final Map<UUID, Reservation> reservations = new HashMap<>();

    public ReservationRepository(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.registerModule(new JavaTimeModule());

        loadData();
    }

    public void loadData(){
        try {
            if (Files.exists(path)) {
                List<Reservation> reservations = mapper.readValue(path.toFile(), new TypeReference<List<Reservation>>() {});

                for (Reservation reservation : reservations) {
                    this.reservations.put(reservation.id(), reservation);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(Reservation reservation) throws IOException {
        reservations.put(reservation.id(), reservation);

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), reservations.values());
    }

    public List<Reservation> listAllUserReservations(UUID userId){
        return reservations.values().stream().filter(reservation -> reservation.passengerId().equals(userId)).toList();
    }

    public Reservation getById(UUID id){
        return reservations.get(id);
    }

    public boolean delete(UUID id) {
        Reservation isRemoved = reservations.remove(id);

        if (isRemoved == null) {
            return false;
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), reservations.values());
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteBySegmentId(List<UUID> segmentIds){
        reservations.values().removeIf(reservation ->
                reservation.itinerary().segments().stream()
                        .anyMatch(segment -> segmentIds.contains(segment.getId()))
        );

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), reservations.values());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
