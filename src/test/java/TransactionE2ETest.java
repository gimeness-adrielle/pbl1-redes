import me.gimenez.domain.models.*;
import me.gimenez.domain.models.Transaction;
import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class TransactionE2ETest {

    private static final String SERVER1 = "http://localhost:7771";
    private static final String SERVER2 = "http://localhost:7772";
    private static final String SERVER3 = "http://localhost:7773";

    private static final DataSetupE2E data = new DataSetupE2E();

    private RestClient c1;
    private RestClient c2;
    private RestClient c3;

    record TestReservationRequest(
            List<Segment> segments,
            User user
    ) {}

    @BeforeEach
    void setup() throws Exception {
        c1 = RestClient.builder().baseUrl(SERVER1).build();
        c2 = RestClient.builder().baseUrl(SERVER2).build();
        c3 = RestClient.builder().baseUrl(SERVER3).build();

        data.reset();

        reload(c1);
        reload(c2);
        reload(c3);
    }

    // Happy path test
    @Test
    void shouldSuccessfullyBook_whenReservationIsValid() throws Exception {
        User user = data.createUser("passageiro@gmail.com");

        List<Segment> segments = List.of(
                data.findSegment(DataSetupE2E.S1),
                data.findSegment(DataSetupE2E.S2),
                data.findSegment(DataSetupE2E.S4)
        );

        // Tries booking segments:
        // Salvador -> Feira de Santana (server1) / Feira de Santana -> Vitoria da Conquista (server2) /
        // Vitoria da Conquista -> Montes Claros (server3)
        Reservation reservation = reserve(c1, segments, user);

        assertThat(reservation).isNotNull();
        assertThat(reservation.passengerId()).isEqualTo(user.id());

        assertSeats(DataSetupE2E.S1, 2);
        assertSeats(DataSetupE2E.S2, 2);
        assertSeats(DataSetupE2E.S4, 2);

        assertNoPendingTransactions();
    }

    // Reject reservation between 3 servers
    @Test
    void shouldRejectReservation_whenNoSeatsAvailable() throws Exception {
        assertSeats(DataSetupE2E.S1, 3);
        assertSeats(DataSetupE2E.S4, 3);

        data.setAvailableSeats(DataSetupE2E.S2, 0);
        reload(c2);

        User user = data.createUser("passageiro@gmail.com");

        List<Segment> segments = List.of(
                data.findSegment(DataSetupE2E.S1),
                data.findSegment(DataSetupE2E.S2),
                data.findSegment(DataSetupE2E.S4)
        );

        assertThatThrownBy(() -> reserve(c1, segments, user))
                .isInstanceOf(RuntimeException.class);

        assertSeats(DataSetupE2E.S2, 0);
        assertNoPendingTransactions();

        for (RestClient client : List.of(c1, c3)) {
            List<Transaction> transactions = client.get()
                    .uri("/test/api/transactions")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            assertThat(transactions)
                    .isNotEmpty()
                    .allMatch(tx ->
                            tx.getCurrentStatus() == TransactionStatus.ABORTED);
        }

        assertSeats(DataSetupE2E.S1, 3);
        assertSeats(DataSetupE2E.S4, 3);
    }

    // Concurrence test
    @Test
    void shouldBookCorrectlyUnderDistributedLoad() throws Exception {
        int requests = 200;

        List<RestClient> clients = List.of(c1, c2, c3);

        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> futures = IntStream.range(0, requests)
                    .mapToObj(i -> executor.submit(() -> {
                        RestClient client = clients.get(i % clients.size());

                        User user = data.createUser("passageiro" + i + "@gmail.com");

                        List<Segment> segments = List.of(data.findSegment(DataSetupE2E.S1));

                        start.await();

                        return tryReserve(client, segments, user);
                    }))
                    .toList();

            start.countDown();

            List<Boolean> results = new ArrayList<>();

            for (Future<Boolean> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            long successfulReservations = results.stream()
                    .filter(Boolean::booleanValue)
                    .count();

            long failedReservations = results.stream()
                    .filter(success -> !success)
                    .count();

            assertThat(successfulReservations).isEqualTo(3);
            assertThat(failedReservations).isEqualTo(requests - 3);

            assertSeats(DataSetupE2E.S1, 0);

            assertNoPendingTransactions();

        } finally {
            executor.shutdownNow();
        }
    }

    // Unavailable server
    @Test
    void shouldRejectReservation_whenParticipantIsUnavailable() throws Exception {
        compose("stop", "server2");

        try {
            User user = data.createUser("passageiro1@gmail.com");

            List<Segment> segments = List.of(
                    data.findSegment(DataSetupE2E.S1),
                    data.findSegment(DataSetupE2E.S2),
                    data.findSegment(DataSetupE2E.S4)
            );

            // Tries booking segments:
            // Salvador -> Feira de Santana (server1) / Feira de Santana -> Vitoria da Conquista (server2) /
            // Vitoria da Conquista -> Montes Claros (server3)
            boolean success = tryReserve(c1, segments, user);

            assertThat(success).isFalse();

            for (RestClient client : List.of(c1, c3)) {
                List<Transaction> transactions = client.get()
                        .uri("/test/api/transactions")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                assertThat(transactions)
                        .isNotEmpty()
                        .allMatch(tx ->
                                tx.getCurrentStatus() == TransactionStatus.ABORTED);
            }


        } finally {
            compose("start", "server2");
        }
    }

    // Precedence criterion test
    @Test
    void shouldRespectPrecedence_whenServersCompeteForLastSeat() throws Exception{
        data.setAvailableSeats(DataSetupE2E.S1, 1);
        reload(c1);

        User user1 = data.createUser("passageiro1@gmail.com");
        User user2 = data.createUser("passageiro2@gmail.com");

        Segment segment = data.findSegment(DataSetupE2E.S1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            CompletableFuture<Boolean> request1 =
                    CompletableFuture.supplyAsync(
                            () -> tryReserve(c2, List.of(segment), user1),
                            executor
                    );

            CompletableFuture<Boolean> request2 =
                    CompletableFuture.supplyAsync(
                            () -> tryReserve(c3, List.of(segment), user2),
                            executor
                    );

            boolean result2 = request2.join();
            Thread.sleep(150);
            boolean result1 = request1.join();

            assertThat(result1 ^ result2)
                    .as("Exatamente um servidor deve conseguir reservar")
                    .isTrue();

            assertSeats(DataSetupE2E.S1, 0);

            assertNoPendingTransactions();

        } finally {
            executor.shutdownNow();
        }
    }

    // Utils
    private Reservation reserve(RestClient client, List<Segment> segments, User user){
        return client.post()
                .uri("/test/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TestReservationRequest(segments, user))
                .retrieve()
                .body(Reservation.class);
    }

    private boolean tryReserve(RestClient client, List<Segment> segments, User user){
        try {
            reserve(client, segments, user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void reload(RestClient client) {
        client.post()
                .uri("/test/api/data/reload")
                .retrieve()
                .toBodilessEntity();
    }

    private void assertSeats(UUID segmentId, int expected) throws IOException {
        Segment segment = data.findSegment(segmentId);

        assertThat(segment.getAvailableSeats()).isEqualTo(expected);
    }

    private void assertNoPendingTransactions() {
        for (RestClient client : List.of(c1, c2, c3)) {
            List<Transaction> transactions = client.get()
                    .uri("/test/api/transactions")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            assertThat(transactions)
                    .noneMatch(tx ->
                            tx.getCurrentStatus() == TransactionStatus.PREPARED
                                    || tx.getCurrentStatus() == TransactionStatus.PREPARING
                                    || tx.getCurrentStatus() == TransactionStatus.COMMITTING);
        }
    }

    private void compose(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("docker", "compose", "-f", "compose-test.yaml"));

        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .directory(new File(System.getProperty("user.dir")))
                .inheritIO()
                .start();

        int exitCode = process.waitFor();

        assertThat(exitCode).isZero();
    }
}