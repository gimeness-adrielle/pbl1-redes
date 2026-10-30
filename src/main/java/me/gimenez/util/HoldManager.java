package me.gimenez.util;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HoldManager {
    private final Map<UUID, Integer> holds = new ConcurrentHashMap<>();

    public void releaseHolds(List<UUID> segmentIds) {
        segmentIds.forEach(id -> holds.computeIfPresent(id, (key, count) -> count <= 1 ? null : count - 1));
    }

    public void hold(List<UUID> segmentIds){
        for (UUID id: segmentIds){
            holds.merge(id, 1, Integer::sum);
        }
    }

    public int getHolds(UUID id) {
        return holds.getOrDefault(id, 0);
    }
}
