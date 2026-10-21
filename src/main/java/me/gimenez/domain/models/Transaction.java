package me.gimenez.domain.models;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public record Transaction(
        UUID id,
        String serverId,
        TransactionStatus status,
        AtomicLong logicalTimeStamp
) {
}
