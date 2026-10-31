package me.gimenez.server.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.dto.TransactionDTO;
import me.gimenez.domain.models.Transaction;
import me.gimenez.domain.models.TransactionDecision;
import me.gimenez.domain.models.TransactionStatus;
import me.gimenez.domain.models.TransactionType;
import me.gimenez.server.rest.PeerClient;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.ReservationNotCreatedException;
import me.gimenez.util.LamportClock;
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
    private final LamportClock lamportClock;
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public boolean remoteCreateReservation(Map<String, List<UUID>> segmentsByServer,
                                        boolean hasLocalSegments, TransactionType transactionType
    ) {
        log.info("Remote reservation applied.");
        long timestamp = lamportClock.tick();
        UUID txId = UUID.randomUUID();

        // Transaction participants
        List<String> participants = segmentsByServer.keySet().stream().toList();

        // PHASE 1: PREPARE
        boolean allPrepared = prepareDecision(txId, segmentsByServer, hasLocalSegments, participants, transactionType, timestamp);

        // ABORT: if not everyone responded with "PREPARED"
        if (!allPrepared) {
            abortDecision(txId, participants, hasLocalSegments);
            return false;
        }

        // PHASE 2: COMMIT
        commitDecision(txId, segmentsByServer, hasLocalSegments, transactionType);
        return true;
    }

    private boolean prepareDecision(UUID txId, Map<String, List<UUID>> segmentsByServer,
                                    boolean hasLocalSegments, List<String> participants,
                                    TransactionType transactionType, long timestamp
    ){
        // If there are local operations, it executes the local prepare step.
        if (hasLocalSegments){
            List<UUID> localSegmentIds = segmentsByServer.get(peerProperties.selfId());

            TransactionStatus status = transactionService.prepare(
                    txId,
                    localSegmentIds,
                    peerProperties.selfId(),
                    participants,
                    transactionType
            );

            boolean preparedLocal = status == TransactionStatus.PREPARED;
            if(!preparedLocal){ return false; }
        } else {
            transactionService.createTransaction(txId, List.of(), peerProperties.selfId(), participants, transactionType);
        }

        List<TransactionDTO.Response> remotePrepares = peerClient.sendPrepare(txId, segmentsByServer, transactionType, timestamp);

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

    private void commitDecision(UUID txId, Map<String, List<UUID>> segmentsByServer,
                                boolean hasLocalSegments, TransactionType transactionType
    ){
        long timestamp = lamportClock.tick();

        try {
            log.info("Transaction {} has been prepared, committing now...", txId);
            Transaction transaction = transactionRepository.findById(txId);

            if (hasLocalSegments){
                transactionService.commit(txId, transactionType);
                transaction.updateParticipantStatus(peerProperties.selfId(), TransactionStatus.COMMITTED);
            }else {
                transaction.setDecision(TransactionDecision.COMMIT);
                transaction.setCurrentStatus(TransactionStatus.COMMITTED);
            }
            transactionRepository.save(transaction);

            retryCommit(txId, new ArrayList<>(segmentsByServer.keySet()), transactionType);
        } catch (IOException e) {
            throw new PersistenceErrorException("[ERROR] Falha ao persistir o commit da transação.", e);
        }
    }

    private void abortDecision(UUID txId, List<String> participants, boolean hasLocalSegments){
        long timestamp = lamportClock.tick();

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

        List<TransactionDTO.Response> responses = peerClient.sendAbort(txId, participants, timestamp);
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

    public void retryCommit(UUID txId, List<String> serversToSend, TransactionType transactionType) {
        Transaction t = transactionRepository.findById(txId);
        if (t == null || t.getCurrentStatus() == TransactionStatus.ABORTED) return;

        log.info("{}: Trying to commit transaction: {}", peerProperties.selfId(), txId);

        List<TransactionDTO.Response> responses = peerClient.sendCommit(txId, serversToSend, transactionType);

        List<String> confirmedServers = responses.stream()
                .filter(response -> response != null &&
                        response.status() == TransactionStatus.COMMITTED)
                .map(TransactionDTO.Response::serverId)
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
            scheduler.schedule(() -> retryCommit(txId, pendingServers, transactionType), 10, TimeUnit.SECONDS);

        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao salvar a transação.", e);
        }
    }

    public TransactionDecision getTxDecision(UUID txId) {
        if (transactionRepository.findById(txId) == null) return null;

        return transactionRepository.findById(txId).getDecision();
    }

}
