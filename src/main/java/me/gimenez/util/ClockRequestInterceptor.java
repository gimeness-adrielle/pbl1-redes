package me.gimenez.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static me.gimenez.util.Config.LAMPORT_HEADER;

@Component
@RequiredArgsConstructor
public class ClockRequestInterceptor implements ClientHttpRequestInterceptor {
    private final LamportClock lamportClock;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long timestamp = lamportClock.tick();

        request.getHeaders().set(LAMPORT_HEADER, String.valueOf(timestamp));
        ClientHttpResponse response = execution.execute(request, body);

        String responseTimestamp = response.getHeaders().getFirst(LAMPORT_HEADER);

        if (responseTimestamp != null) {
            try {
                lamportClock.update(Long.parseLong(responseTimestamp));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Timestamp lógico inválido recebido do servidor.", e);
            }
        }

        return response;
    }

}
