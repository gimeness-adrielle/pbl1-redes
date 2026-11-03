package me.gimenez.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class LamportClock {
    private final AtomicLong clock = new AtomicLong(0);

    public long tick() {
        return clock.incrementAndGet();
    }

    public long update(long receivedTimestamp) {
        return clock.updateAndGet(current -> Math.max(current, receivedTimestamp) + 1);
    }
    
}
