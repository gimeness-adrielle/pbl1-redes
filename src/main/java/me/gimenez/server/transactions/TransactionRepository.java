package me.gimenez.server.transactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.util.PeerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
    private final ObjectMapper mapper;
    private final PeerProperties peerProperties;

    @Value("${app.storage.path}")
    private String storagePath;
    private Path path;

    @Getter
    private final Map<UUID, Transaction> transactions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.path = Path.of(storagePath, "transactions.json");
        reload();
    }

    public synchronized void reload() {
        transactions.clear();

        try {
            if (Files.exists(path)) {
                List<Transaction> transactions = mapper.readValue(path.toFile(), new TypeReference<>() {});

                for (Transaction transaction : transactions) {
                    this.transactions.put(transaction.getTxId(), transaction);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void save(Transaction transaction) throws IOException {
        transactions.put(transaction.getTxId(), transaction);
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), transactions.values());
    }

    public List<Transaction> getPendingTransactions() {
        Set<UUID> pendingIds = new HashSet<>();

        for (Transaction tx : transactions.values()) {
            if (isPending(tx.getCurrentStatus())) {
                pendingIds.add(tx.getTxId());
            }

            if (tx.getCoordinatorId().equals(peerProperties.selfId())
                    && tx.getParticipants() != null
                    && tx.getParticipants().values().stream().anyMatch(this::isPending)) {
                pendingIds.add(tx.getTxId());
            }
        }

        return pendingIds.stream()
                .map(transactions::get)
                .toList();
    }

    private boolean isPending(TransactionStatus status){
        return status.equals(TransactionStatus.PREPARING)
                || status.equals(TransactionStatus.PREPARED)
                || status.equals(TransactionStatus.COMMITTING);
    }

    public Transaction findById(UUID txId) {
        return transactions.get(txId);
    }

    public Collection<Transaction> getAllTransactions() {
        return transactions.values();
    }

}
