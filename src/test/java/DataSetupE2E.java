import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import me.gimenez.domain.models.Ride;
import me.gimenez.domain.models.Segment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DataSetupE2E {

    private static final Path dir = Path.of("./data-test");
    private static final Path ridesFileServer1 = dir.resolve("server1/rides.json");
    private static final Path ridesFileServer2 = dir.resolve("server2/rides.json");
    private static final Path ridesFileServer3 = dir.resolve("server3/rides.json");

    private final ObjectMapper mapper;

    @Getter
    private final List<Segment> segments;

    public DataSetupE2E() throws IOException {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        cleanUpData();
        this.segments = setupInitialData();
    }

    void cleanUpData() throws IOException {
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {}
                        });
            }
        }
        Files.createDirectories(dir.resolve("server1"));
        Files.createDirectories(dir.resolve("server2"));
        Files.createDirectories(dir.resolve("server3"));
    }

    private List<Segment> setupInitialData() throws IOException {
        UUID S1_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID S2_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID S3_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID S4_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID S5_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

        UUID R1_ID = UUID.fromString("10000000-0000-0000-0000-000000000000");
        UUID R2_ID = UUID.fromString("20000000-0000-0000-0000-000000000000");
        UUID R3_ID = UUID.fromString("30000000-0000-0000-0000-000000000000");
        UUID R4_ID = UUID.fromString("40000000-0000-0000-0000-000000000000");
        UUID R5_ID = UUID.fromString("50000000-0000-0000-0000-000000000000");

        // Server 1 segments
        Segment s1 = new Segment(
                S1_ID,
                "server-1",
                "Salvador", "Feira De Santana",
                60,
                3,
                LocalDateTime.of(2029, 11, 3, 7, 30),
                LocalDateTime.of(2029, 11, 3, 8, 30)
        );

        // Server 2 segments
        Segment s2 = new Segment(
                S2_ID,
                "server-2",
                "Feira De Santana", "Vitoria Da Conquista",
                80,
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                LocalDateTime.of(2029, 11, 3, 14, 30)
        );

        Segment s3 = new Segment(
                S3_ID,
                "server-2",
                "Serrinha", "Feira De Santana",
                80,
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                LocalDateTime.of(2029, 11, 3, 14, 30)
        );

        // Server 3 segments
        Segment s4 = new Segment(
                S4_ID,
                "server-3",
                "Vitoria Da Conquista", "Montes Claros",
                100,
                3,
                LocalDateTime.of(2029, 11, 3, 15, 30),
                LocalDateTime.of(2029, 11, 3, 20, 30)
        );

        Segment s5 = new Segment(
                S5_ID,
                "server-3",
                "Feira De Santana", "Salvador",
                80,
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                LocalDateTime.of(2029, 11, 3, 14, 30)
        );

        Ride r1 = new Ride(
                R1_ID,
                UUID.randomUUID(),
                3,
                LocalDateTime.of(2029, 11, 3, 7, 30),
                List.of(s1)
        );

        Ride r2 = new Ride(
                R2_ID,
                UUID.randomUUID(),
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                List.of(s2)
        );

        Ride r3 = new Ride(
                R3_ID,
                UUID.randomUUID(),
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                List.of(s3)
        );

        Ride r4 = new Ride(
                R4_ID,
                UUID.randomUUID(),
                3,
                LocalDateTime.of(2029, 11, 3, 15, 30),
                List.of(s4)
        );

        Ride r5 = new Ride(
                R5_ID,
                UUID.randomUUID(),
                3,
                LocalDateTime.of(2029, 11, 3, 9, 30),
                List.of(s5)
        );

        mapper.writerWithDefaultPrettyPrinter().writeValue(ridesFileServer1.toFile(), List.of(r1));
        mapper.writerWithDefaultPrettyPrinter().writeValue(ridesFileServer2.toFile(), List.of(r2, r3));
        mapper.writerWithDefaultPrettyPrinter().writeValue(ridesFileServer3.toFile(), List.of(r4, r5));

        return List.of(s1, s2, s3, s4, s5);
    }

    void saveSegment(Segment segment, String serverId) throws IOException {
        switch (serverId) {
            case "server-1":
                mapper.writeValue(ridesFileServer1.toFile(), List.of(segment));
                break;
            case "server-2":
                mapper.writeValue(ridesFileServer2.toFile(), List.of(segment));
                break;
            case "server-3":
                mapper.writeValue(ridesFileServer3.toFile(), List.of(segment));
                break;
        }
    }
}
