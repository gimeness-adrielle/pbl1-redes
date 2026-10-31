package me.gimenez.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class LamportClock {

    private final AtomicLong clock = new AtomicLong(0);

    // Evento local: incrementa o relógio
    public long tick() {
        return clock.incrementAndGet();
    }

    // Evento recebido: atualiza usando o timestamp recebido
    public long update(long receivedTimestamp) {
        return clock.updateAndGet(
                current -> Math.max(current, receivedTimestamp) + 1
        );
    }

    // Consulta o valor atual sem incrementar
    public long getTimestamp() {
        return clock.get();
    }
}
