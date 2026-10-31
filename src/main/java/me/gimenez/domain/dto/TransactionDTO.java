package me.gimenez.domain.dto;

import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.domain.models.TransactionType;

import java.util.List;
import java.util.UUID;

public final class TransactionDTO {

    public record Request(
            UUID txId,
            String coordinatorId,
            long logicalTimestamp,
            List<UUID> segmentIds,
            TransactionType transactionType
    ) {}

    public record Response(
            UUID txId,
            TransactionStatus status,
            String serverId,
            long logicalTimestamp
    ) {}

}
