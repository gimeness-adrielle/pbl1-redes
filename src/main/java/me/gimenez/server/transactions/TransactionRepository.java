package me.gimenez.server.transactions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransactionRepository {
    private final ObjectMapper mapper;

    @Value("${app.storage.path}")
    private String storagePath;
    private Path path;

    @Getter
    private final Map<UUID, Transaction> transactions = new ConcurrentHashMap<>();

    public TransactionRepository(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void init(){
        this.path = Path.of(storagePath, "transactions.json");
        loadData();
    }

    private void loadData(){
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
        return transactions.values().stream()
                .filter(transaction ->
                        transaction.getCurrentStatus() == TransactionStatus.PREPARING
                                || transaction.getCurrentStatus() == TransactionStatus.PREPARED
                                || transaction.getCurrentStatus() == TransactionStatus.COMMITTING
                )
                .toList();
    }

    public Transaction findById(UUID txId) {
        return transactions.get(txId);
    }

}
