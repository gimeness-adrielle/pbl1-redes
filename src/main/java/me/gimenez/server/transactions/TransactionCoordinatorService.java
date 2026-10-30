package me.gimenez.server.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.responses.TransactionResponse;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionDecision;
import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.domain.models.TransactionType;
import me.gimenez.server.communication.PeerClient;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.util.HoldManager;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionCoordinatorService {
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void remoteReservation(Map<String, List<UUID>> segmentsByServer, boolean hasLocalSegments) {
        log.info("Remote reservation applied.");
        UUID txId = UUID.randomUUID();

        // Transaction participants
        List<String> participants = segmentsByServer.keySet().stream().toList();

        // PHASE 1: PREPARE
        boolean allPrepared = prepareDecision(txId, segmentsByServer, hasLocalSegments, participants);

        // ABORT: if not everyone responded with "PREPARED"
        if (!allPrepared) {
            abortDecision(txId, participants, hasLocalSegments);
            return;
        }

        // PHASE 2: COMMIT
        commitDecision(txId, segmentsByServer, hasLocalSegments);
    }


    private boolean prepareDecision(UUID txId, Map<String, List<UUID>> segmentsByServer,
                                    boolean hasLocalSegments, List<String> participants
    ){
        // If there are local operations, it executes the local prepare step.
        if (hasLocalSegments){
            List<UUID> localSegmentIds = segmentsByServer.get(peerProperties.selfId());

            TransactionStatus status = transactionService.prepare(
                    txId,
                    localSegmentIds,
                    peerProperties.selfId(),
                    participants
            );

            boolean preparedLocal = status == TransactionStatus.PREPARED;
            if(!preparedLocal){ return false; }
        } else {
            transactionService.createTransaction(txId, List.of(), peerProperties.selfId(), participants);
        }

        List<TransactionResponse> remotePrepares = peerClient.sendPrepare(txId, segmentsByServer);

        Transaction transaction = transactionRepository.findById(txId);

        remotePrepares.forEach(response -> {
            if (response != null) {
                transaction.updateParticipantStatus(response.serverId(), response.status());
            }
        });

        try {
            transactionRepository.save(transaction);
        } catch (IOException e) {
            throw new PersistenceErrorException("[ERROR] Falha ao persistir o commit da transação.", e);
        }

        return transaction.getParticipants().values().stream()
                .allMatch(status -> status.equals(TransactionStatus.PREPARED));
    }

    private void commitDecision(UUID txId, Map<String, List<UUID>> segmentsByServer, boolean hasLocalSegments){
        try {
            log.info("Transaction {} has been prepared, committing now...", txId);
            Transaction transaction = transactionRepository.findById(txId);

            if (hasLocalSegments){
                transactionService.commit(txId);
                transaction.updateParticipantStatus(peerProperties.selfId(), TransactionStatus.COMMITTED);
            }else {
                transaction.setDecision(TransactionDecision.COMMIT);
                transaction.setCurrentStatus(TransactionStatus.COMMITTED);
            }
            transactionRepository.save(transaction);

            retryCommit(txId, new ArrayList<>(segmentsByServer.keySet()));
        } catch (IOException e) {
            throw new PersistenceErrorException("[ERROR] Falha ao persistir o commit da transação.", e);
        }
    }

    private void abortDecision(UUID txId, List<String> participants, boolean hasLocalSegments){
        log.info("Not everyone has confirmed they are prepared; aborting operation...");
        Transaction transaction = transactionRepository.findById(txId);
        transaction.setDecision(TransactionDecision.ABORT);

        try {
            if (hasLocalSegments){
                transactionService.abort(txId);
                transaction.updateParticipantStatus(peerProperties.selfId(), TransactionStatus.ABORTED);
            }else {
                transaction.setCurrentStatus(TransactionStatus.ABORTED);
            }
            transactionRepository.save(transaction);
        }catch (IOException e){
            throw new PersistenceErrorException("[ERROR] Falha ao persistir o commit da transação.", e);
        }

        List<TransactionResponse> responses = peerClient.sendDecision(txId, "abort", participants);
        responses.forEach(response -> {
            if (response != null){
                transaction.updateParticipantStatus(response.serverId(), response.status());
            }
        });

        try {
            transactionRepository.save(transaction);
            log.info("Transaction {} has been aborted", txId);
        } catch (IOException e) {
            throw new PersistenceErrorException("[ERROR] Falha ao persistir o commit da transação.", e);
        }

        throw new ReservationNotCreatedException("Não foi possível reservar o itinerário. Tente novamente mais tarde.");
    }

    public void retryCommit(UUID txId, List<String> serversToSend) {
        Transaction t = transactionRepository.findById(txId);
        if (t == null || t.getCurrentStatus() == TransactionStatus.ABORTED) return;

        log.info("{}: Trying to commit transaction: {}", peerProperties.selfId(), txId);

        List<TransactionResponse> responses = peerClient.sendDecision(txId, "commit", serversToSend);

        List<String> confirmedServers = responses.stream()
                .filter(response -> response != null &&
                        response.status() == TransactionStatus.COMMITTED)
                .map(TransactionResponse::serverId)
                .toList();
        log.info("Confirmed servers: {}", confirmedServers);

        confirmedServers.forEach(serverId ->
                t.updateParticipantStatus(serverId, TransactionStatus.COMMITTED));

        try {
            transactionRepository.save(t);

            List<String> pendingServers = t.getParticipants().entrySet().stream()
                    .filter(entry -> entry.getValue() != TransactionStatus.COMMITTED)
                    .map(Map.Entry::getKey)
                    .toList();
            log.debug("Pending servers: {}", pendingServers);

            if (pendingServers.isEmpty()) {
                t.setCurrentStatus(TransactionStatus.COMMITTED);
                transactionRepository.save(t);
                log.info("Transaction {} has been committed successfully", txId);
                return;
            }
            log.info("Still have pending commits from transaction {}. Retrying commit in seconds...", txId);
            scheduler.schedule(() -> retryCommit(txId, pendingServers), 10, TimeUnit.SECONDS);

        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao salvar a transação.", e);
        }
    }

    public TransactionDecision getTxDecision(UUID txId) {
        if (transactionRepository.findById(txId) == null) return null;

        return transactionRepository.findById(txId).getDecision();
    }

}
