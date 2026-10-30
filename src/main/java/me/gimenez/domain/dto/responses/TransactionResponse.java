package me.gimenez.domain.dto.responses;

import me.gimenez.domain.models.TransactionStatus;

import java.util.UUID;

public record TransactionResponse(
        UUID txId,
        TransactionStatus status,
        String serverId,
        long logicalTimestamp
) {}