package me.gimenez.util;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LockService {
    private final Map<UUID, ReentrantLock> segmentLocks = new ConcurrentHashMap<>();

    public ReentrantLock getLock (UUID segmentId){
        return segmentLocks.computeIfAbsent(segmentId, _ -> new ReentrantLock());
    }

    public List<ReentrantLock> lock (List<UUID> segmentIds){
        List<UUID> sortedSegmentIds = segmentIds.stream().sorted().toList();    // Ordena os IDs para resolver deadlocks
        List<ReentrantLock> locks = sortedSegmentIds.stream().map(this::getLock).toList();

        locks.forEach(ReentrantLock::lock);

        return locks;
    }

}
