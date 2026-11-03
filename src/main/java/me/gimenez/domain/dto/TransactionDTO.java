package me.gimenez.domain.dto;

import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionDecision;
import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.domain.models.TransactionType;

import java.util.List;
import java.util.UUID;

public final class TransactionDTO {

    public record Request(
            UUID txId,
            String coordinatorId,
            List<UUID> segmentIds,
            TransactionType transactionType,
            long timestamp
    ) {

        public static Transaction from(TransactionDTO.Request request){
            return new Transaction(
                    request.txId(),
                    request.coordinatorId(),
                    request.segmentIds(),
                    request.transactionType(),
                    request.timestamp(),
                    null
            );
        }
    }

    public record Response(
            UUID txId,
            TransactionStatus status,
            String serverId
    ) {}

    public record HealthResponse(
            UUID txId,
            TransactionDecision decision
    ){}

}
