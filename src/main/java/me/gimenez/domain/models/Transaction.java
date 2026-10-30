package me.gimenez.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Transaction{
    private final UUID txId;

    private final String coordinatorId;

    private TransactionDecision decision;

    @Getter @Setter
    private TransactionStatus currentStatus;

    @Getter
    private final TransactionType type;

    @Setter
    private Map<String, TransactionStatus> participants = new ConcurrentHashMap<>();

    @Setter
    private List<UUID> segmentIds;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Transaction(
            @JsonProperty("txId") UUID txId,
            @JsonProperty("coordinatorId") String coordinatorId,
            @JsonProperty("segmentIds") List<UUID> segmentIds,
            @JsonProperty("type") TransactionType type
    ){
        this.txId = txId;
        this.coordinatorId = coordinatorId;
        this.segmentIds = segmentIds;
        this.decision = TransactionDecision.PREPARE;
        this.currentStatus = TransactionStatus.PREPARED;
        this.type = type;
    }

    public synchronized TransactionDecision getDecision() {
        return decision;
    }
    public synchronized void setDecision(TransactionDecision decision) { this.decision=decision; }

    public synchronized void updateParticipantStatus(String serverId, TransactionStatus status){
        this.getParticipants().put(serverId, status);
    }
}
