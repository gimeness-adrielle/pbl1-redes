package me.gimenez.server.reservations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import me.gimenez.domain.models.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ReservationRepository {
    private final ObjectMapper mapper;

    @Value("${app.storage.path}")
    private String storagePath;
    private Path path;

    private final Map<UUID, Reservation> reservations = new HashMap<>();

    public ReservationRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        this.path = Path.of(storagePath, "reservations.json");
        reload();
    }

    public synchronized void reload() {
        reservations.clear();

        try {
            if (Files.exists(path)) {
                List<Reservation> reservations = mapper.readValue(path.toFile(), new TypeReference<>() {});

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

    public boolean delete(UUID id) throws IOException {
        Reservation isRemoved = reservations.remove(id);

        if (isRemoved == null) {
            return false;
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), reservations.values());
        return true;
    }

    public void deleteBySegmentId(List<UUID> segmentsToDelete) throws IOException {
        reservations.values().removeIf(reservation -> reservation.segments().stream()
                        .anyMatch(segment -> segmentsToDelete.contains(segment.getId())));

        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), reservations.values());
    }

    public List<Reservation> findReservationBySegments(List<UUID> segmentsIds){
        Set<UUID> idsSet = Set.copyOf(segmentsIds);

        return reservations.values().stream()
                .filter(reservation -> reservation.segments().stream()
                        .anyMatch(segment -> idsSet.contains(segment.getId())))
                .toList();
    }

    public synchronized void saveIfAbsent(Reservation reservation) throws IOException {
        if (reservations.containsKey(reservation.id())) {return;}
        save(reservation);
    }

}
