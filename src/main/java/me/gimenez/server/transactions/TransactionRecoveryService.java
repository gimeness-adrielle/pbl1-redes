package me.gimenez.server.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.models.Transaction;
import me.gimenez.server.rest.PeerClient;
import me.gimenez.util.HoldManager;
import me.gimenez.util.PeerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRecoveryService {
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;
    private final HoldManager holdManager;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final TransactionCoordinatorService coordinatorService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverTransactions() {
        log.info("Starting transaction recovery...");
        readHolds();
        retryTransactions();
    }

    public void readHolds(){
        List<Transaction> pendingTransactions = transactionRepository.getPendingTransactions();

        for (Transaction transaction : pendingTransactions) {
            holdManager.hold(transaction.getSegmentIds());
        }
    }

    public void retryTransactions(){
        log.info("Searching for pending transactions from last time...");
        List<Transaction> pendingTransactions = transactionRepository.getPendingTransactions();

        if (pendingTransactions.isEmpty()) {
            log.info("Server has not pending transactions from last time.");
            return;
        }

        log.info("Found {} pending transactions.", pendingTransactions.size());
        for (Transaction transaction : pendingTransactions) {
            if (transaction.getCoordinatorId().equals(peerProperties.selfId())){
                log.info("Sending abort/commit for pending transactions...");
                switch (transaction.getDecision()) {
                    case PREPARE:{
                        transactionService.abort(transaction.getTxId());
                        peerClient.sendDecision(transaction.getTxId(), transaction.getParticipants().keySet().stream().toList(), "abort");
                        break;
                    }
                    case COMMIT:
                        coordinatorService.retryCommit(
                                transaction.getTxId(),
                                transaction.getParticipants().keySet().stream().toList()
                        );
                }
            } else{
                log.info("Asking status to coordinator...");
                transactionService.resolveExpired(transaction.getTxId());
            }
        }
    }

}
