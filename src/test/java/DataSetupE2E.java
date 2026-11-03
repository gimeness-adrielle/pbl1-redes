import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.domain.models.UserType;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public class DataSetupE2E {

    static final Path DIR = Path.of("./data-test");

    // Segments IDs
    static final UUID S1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID S2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID S3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID S4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID S5 = UUID.fromString("00000000-0000-0000-0000-000000000005");

    // Rides IDs
    static final UUID R1 = UUID.fromString("10000000-0000-0000-0000-000000000000");
    static final UUID R2 = UUID.fromString("20000000-0000-0000-0000-000000000000");
    static final UUID R3 = UUID.fromString("30000000-0000-0000-0000-000000000000");
    static final UUID R4 = UUID.fromString("40000000-0000-0000-0000-000000000000");
    static final UUID R5 = UUID.fromString("50000000-0000-0000-0000-000000000000");

    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    void reset() throws IOException {
        deleteDirectory();
        createDirectories();
        setupInitialData();
    }

    private void deleteDirectory() throws IOException {
        if (!Files.exists(DIR)) return;

        try (Stream<Path> paths = Files.walk(DIR)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void createDirectories() throws IOException {
        for (String server : List.of("server1", "server2", "server3")) {
            Files.createDirectories(DIR.resolve(server));
        }
    }

    private void setupInitialData() throws IOException {
        Segment s1 = segment(S1, "server-1",
                "Salvador", "Feira De Santana", 60, 3, 7, 30, 8, 30);

        Segment s2 = segment(S2, "server-2",
                "Feira De Santana", "Vitoria Da Conquista", 80, 3, 9, 30, 14, 30);

        Segment s3 = segment(S3, "server-2",
                "Serrinha", "Feira De Santana", 80, 3, 9, 30, 14, 30);

        Segment s4 = segment(S4, "server-3",
                "Vitoria Da Conquista", "Montes Claros", 100, 3, 15, 30, 20, 30);

        Segment s5 = segment(S5, "server-3",
                "Feira De Santana", "Salvador", 80, 3, 9, 30, 14, 30);

        writeRides("server1", List.of(ride(R1, s1)));
        writeRides("server2", List.of(ride(R2, s2), ride(R3, s3)));
        writeRides("server3", List.of(ride(R4, s4), ride(R5, s5)));
    }

    User createUser(String email) {
        return new User(UUID.randomUUID(), email, "passageiro", "12345678", UserType.PASSENGER);
    }

    void setAvailableSeats(UUID segmentId, int seats) throws IOException {
        for (String server : List.of("server1", "server2", "server3")) {
            List<Ride> rides = readRides(server);

            for (Ride ride : rides) {
                ride.segments().stream()
                        .filter(segment -> segment.getId().equals(segmentId))
                        .findFirst()
                        .ifPresent(segment -> segment.setAvailableSeats(seats));
            }

            writeRides(server, rides);
        }
    }

    List<Ride> readRides(String server) throws IOException {
        Path path = DIR.resolve(server).resolve("rides.json");

        return mapper.readValue(path.toFile(), new TypeReference<>() {});
    }

    Segment findSegment(UUID id) throws IOException {
        for (String server : List.of("server1", "server2", "server3")) {
            for (Ride ride : readRides(server)) {
                for (Segment segment : ride.segments()) {
                    if (segment.getId().equals(id)) {
                        return segment;
                    }
                }
            }
        }

        throw new IllegalArgumentException("Segmento não encontrado: " + id);
    }

    private Segment segment(UUID id, String server, String origin, String destination, int price,
                            int seats, int startHour, int startMinute, int endHour, int endMinute) {
        return new Segment(
                id, server,
                origin, destination,
                price, seats,
                LocalDateTime.of(2029, 11, 3, startHour, startMinute),
                LocalDateTime.of(2029, 11, 3, endHour, endMinute)
        );
    }

    private Ride ride(UUID id, Segment segment) {
        return new Ride(
                id,
                UUID.randomUUID(),
                3,
                segment.getDepartureAt(),
                List.of(segment)
        );
    }

    private void writeRides(String server, List<Ride> rides)
            throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(
                        DIR.resolve(server).resolve("rides.json").toFile(),
                        rides
                );
    }
}