package me.gimenez.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.dto.TransactionDTO;
import me.gimenez.util.LamportClock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Transaction implements Comparable<Transaction> {
    private final UUID txId;

    private final String coordinatorId;

    private TransactionDecision decision;

    @Setter
    private long logicalTimestamp;

    @Getter @Setter
    private TransactionStatus currentStatus;

    @Getter
    private final TransactionType type;

    @Setter
    @JsonProperty("participants")
    private Map<String, TransactionStatus> participants = new ConcurrentHashMap<>();

    @Setter
    private List<UUID> segmentIds;

    @Getter
    private final ReservationDTO.Data reservationData;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Transaction(
            @JsonProperty("txId") UUID txId,
            @JsonProperty("coordinatorId") String coordinatorId,
            @JsonProperty("segmentIds") List<UUID> segmentIds,
            @JsonProperty("type") TransactionType type,
            @JsonProperty("logicalTimestamp") long logicalTimestamp,
            @JsonProperty("reservationData") ReservationDTO.Data reservationData
    ){
        this.txId = txId;
        this.coordinatorId = coordinatorId;
        this.segmentIds = segmentIds;
        this.decision = TransactionDecision.PREPARE;
        this.currentStatus = TransactionStatus.PREPARED;
        this.type = type;
        this.logicalTimestamp = logicalTimestamp;
        this.reservationData = reservationData;
    }

    public synchronized TransactionDecision getDecision() {
        return decision;
    }
    public synchronized void setDecision(TransactionDecision decision) { this.decision=decision; }

    public synchronized void updateParticipantStatus(String serverId, TransactionStatus status){
        this.getParticipants().put(serverId, status);
    }

    @Override
    public int compareTo(Transaction otherTransaction) {
        int compTimestamp = Long.compare(this.logicalTimestamp, otherTransaction.logicalTimestamp);
        if (compTimestamp != 0) {
            return compTimestamp;
        }
        return this.coordinatorId.compareTo(otherTransaction.coordinatorId);
    }

}
