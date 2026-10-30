package me.gimenez.domain.dto.requests;

import java.util.List;
import java.util.UUID;

public record TransactionRequest(
        UUID txId,
        String coordinatorId,
        long logicalTimestamp,
        List<UUID> segmentIds
) {
}
