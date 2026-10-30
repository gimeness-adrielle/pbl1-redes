
import me.gimenez.domain.models.Segment;
import me.gimenez.domain.models.User;
import me.gimenez.domain.models.UserType;
import me.gimenez.server.communication.TestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionE2ETest extends DataSetupE2E {
    private RestClient clientServer1;
    private RestClient clientServer2;
    private RestClient clientServer3;

    private List<Segment> segments;

    public TransactionE2ETest() throws IOException {}

    @BeforeEach
    void setUp() {
        this.clientServer1 = RestClient.builder().baseUrl("http://localhost:7771").build();
        this.clientServer2 = RestClient.builder().baseUrl("http://localhost:7772").build();
        this.clientServer3 = RestClient.builder().baseUrl("http://localhost:7773").build();

        this.segments = getSegments();
    }

    User createUser(String email){
        return new User(UUID.randomUUID(), email, "passageiro", "12345678", UserType.PASSENGER);
    }

    @Test
    void shouldSuccessfullyBook_whenTheReservationIsValid(){
        // Reservation request (Salvador -> Montes Claros)
        // Route: Salvador (Server 1) -> Feira De Santana (Server 1/2) -> Vitoria Da Conquista (Server 2/3) -> Montes Claros (Server 3)
        var request = new TestController.TestReservationRequest(
                List.of(segments.get(0),  segments.get(1), segments.get(3)),
                createUser("passageiro1@gmail.com")
        );

        var response = clientServer1.post()
            .uri("/test/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

}
