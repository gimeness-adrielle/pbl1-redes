package me.gimenez.server.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.ReservationDTO;
import me.gimenez.domain.models.Transaction;
import me.gimenez.server.reservations.ReservationService;
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
    private final ReservationService reservationService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverTransactions() {
        log.info("Starting transaction recovery...");

        log.info("Searching for pending transactions from last time...");
        List<Transaction> pendingTransactions = transactionRepository.getPendingTransactions();

        if (pendingTransactions.isEmpty()) {
            log.info("Server has not pending transactions from last time.");
            return;
        }

        log.info("Found {} pending transactions.", pendingTransactions.size());

        readHolds(pendingTransactions);
        retryTransactions(pendingTransactions);
    }

    public void readHolds(List<Transaction> pendingTransactions){
        for (Transaction tx : pendingTransactions) {
            if (tx.getSegmentIds() != null){
                holdManager.hold(tx.getSegmentIds());
            }
        }
    }

    public void retryTransactions(List<Transaction> pendingTransactions){
        for (Transaction tx : pendingTransactions) {
            if (tx.getCoordinatorId().equals(peerProperties.selfId())){
                log.info("Sending abort/commit for pending transactions...");

                List<String> participants = tx.getParticipants().keySet().stream().toList();
                switch (tx.getDecision()) {
                    case PREPARE:{
                        transactionService.abort(tx.getTxId());
                        peerClient.sendDecision(tx.getTxId(), participants, "abort");
                        break;
                    }
                    case COMMIT:
                        coordinatorService.retryCommit(tx.getTxId(), participants);
                        ReservationDTO.Data reservationData = tx.getReservationData();
                        reservationService.buildReservation(reservationData.reservationId(), reservationData.segments(), reservationData.userId());
                }
            } else{
                log.info("Asking status to coordinator...");
                transactionService.resolveExpired(tx.getTxId());
            }
        }
    }

}
