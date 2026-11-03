package me.gimenez.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import static me.gimenez.util.Config.LAMPORT_HEADER;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClockResponseInterceptor implements HandlerInterceptor {
    private final LamportClock lamportClock;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("X-Lamport-Timestamp");

        if (header != null) {
            try {
                long receivedTimestamp = Long.parseLong(header);

                long novoValorLocal = lamportClock.update(receivedTimestamp);

                System.out.println("[Lamport] Sincronizado. Recebido: " + receivedTimestamp + " -> Novo Local: " + novoValorLocal);

            } catch (NumberFormatException e) {
                log.error("[Lamport] Erro ao parsear timestamp recebido: {}", header);
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        long timestamp = lamportClock.tick();

        response.setHeader(LAMPORT_HEADER, String.valueOf(timestamp));

        log.info("[Lamport] Enviando resposta com timestamp: {}", timestamp);
    }
}
