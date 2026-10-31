package me.gimenez.server.transactions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.gimenez.domain.models.*;
import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.TransactionNotFoundException;
import me.gimenez.server.exceptions.TransactionRejectedException;
import me.gimenez.server.rides.RideRepository;
import me.gimenez.server.rest.PeerClient;
import me.gimenez.util.HoldManager;
import me.gimenez.util.LockService;
import me.gimenez.util.PeerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TransactionRepository transactionRepository;
    private final RideRepository rideRepository;
    private final LockService lockService;
    private final HoldManager holdManager;

    /**
     * This function ensures that the given segments it's available for booking. It checks the holds numbers and seats.
     * @param segments Segments to be checked.
     * @return True if all the segments are available, otherwise returns false.
     */
    public boolean checkAvailability(List<Segment> segments){
        return segments.stream().allMatch(
                segment -> {
                    int holdsNum = holdManager.getHolds(segment.getId());
                    return segment.getAvailableSeats() - holdsNum > 0;
                });
    }

    public TransactionStatus prepare(UUID txId, List<UUID> segmentIds, String coordinatorId,
                                     List<String> participants, TransactionType transactionType){
        log.info("{} PREPARING: preparing transaction {}", peerProperties.selfId().toUpperCase(), txId);
        List<ReentrantLock> locks = lockService.lock(segmentIds);

        try {
            // If the given transaction already exists.
            Transaction existing = transactionRepository.findById(txId);
            if (existing != null){
                log.info("Transaction {} already exists.", txId);
                return existing.getCurrentStatus();
            }

            // Ensures that segments that are being manipulated is fetched to repository data.
            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null) {
                throw new TransactionRejectedException("Um ou mais segmentos não foram encontrados.");
            }

            if (transactionType == TransactionType.CREATE_RESERVATION && !checkAvailability(currentSegments)){
                throw new TransactionRejectedException("Não há assentos disponíveis para todos os segmentos.");
            }

            Transaction transaction = createTransaction(txId, segmentIds, coordinatorId, participants, transactionType);
            log.info("Status: {}", transaction.getCurrentStatus());

            if (!coordinatorId.equals(peerProperties.selfId())) {
                scheduler.schedule(() -> resolveExpired(txId, transactionType), 15, TimeUnit.SECONDS);
            }

            log.info("{} PREPARE: transaction {} has been prepared.\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());
            return TransactionStatus.PREPARED;
        }finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public Transaction commit (UUID txId, TransactionType transactionType){
        Transaction transaction = transactionRepository.findById(txId);
        if (transaction == null){
            throw new TransactionNotFoundException("Não foi encontrada uma transação com este id.");
        }

        log.info("{} COMMITING: transaction {}\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());
        transaction.setDecision(TransactionDecision.COMMIT);
        transaction.setCurrentStatus(TransactionStatus.COMMITTING);
        try{
            transactionRepository.save(transaction);
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao persistir o commit da transação.", e);
        }

        List<UUID> segmentIds = transaction.getSegmentIds();

        List<ReentrantLock> locks = lockService.lock(segmentIds);

        try {
            if (transaction.getCurrentStatus() != TransactionStatus.PREPARED
                    && transaction.getCurrentStatus() != TransactionStatus.COMMITTING) {
                log.info("This transaction has already been {}.", transaction.getCurrentStatus().toString().toLowerCase());
                return transaction;
            }

            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null) {
                throw new TransactionRejectedException("Um ou mais segmentos não foram encontrados.");
            }

            if (transactionType == TransactionType.CREATE_RESERVATION){
                for (Segment segment : currentSegments) {
                    segment.setAvailableSeats(segment.getAvailableSeats() - 1);
                }
            } else {
                for (Segment segment : currentSegments) {
                    segment.setAvailableSeats(segment.getAvailableSeats() + 1);
                }
            }
            rideRepository.saveAll();

            holdManager.releaseHolds(segmentIds);
            transaction.setCurrentStatus(TransactionStatus.COMMITTED);
            transactionRepository.save(transaction);

            log.info("{} COMMIT: transaction {} has been committed.\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());
            return transaction;
        } catch(IOException e) {
            throw new PersistenceErrorException("Falha ao persistir o commit da transação.", e);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public TransactionStatus abort(UUID txId){
        Transaction transaction = transactionRepository.findById(txId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Não foi encontrada uma transação com este id.");
        }

        transaction.setDecision(TransactionDecision.ABORT);
        log.info("{} ABORTING: transaction {}\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());

        List<UUID> segmentIds = transaction.getSegmentIds();

        List<ReentrantLock> locks = lockService.lock(segmentIds);

        try {
            if (transaction.getCurrentStatus() == TransactionStatus.ABORTED
                    || transaction.getCurrentStatus() == TransactionStatus.COMMITTING
                    || transaction.getCurrentStatus() == TransactionStatus.COMMITTED
            ) {
                return transaction.getCurrentStatus();
            }

            holdManager.releaseHolds(segmentIds);
            transaction.setCurrentStatus(TransactionStatus.ABORTED);

            transactionRepository.save(transaction);

            log.info("{} ABORT: transaction {} has been aborted.\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());
            return transaction.getCurrentStatus();
        }catch (IOException e) {
            throw new PersistenceErrorException("Falha ao persistir a transação.", e);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public Transaction createTransaction (UUID txId, List<UUID> segmentIds, String coordinatorId,
                                          List<String> participants, TransactionType transactionType){
        log.info("Creating new transaction: {}", txId);
        Map<String, TransactionStatus> participantsStatus = new HashMap<>();

        Transaction transaction;

        if (participants != null){
            for (String serverId: participants){
                participantsStatus.put(serverId, TransactionStatus.PREPARING);
            }
            transaction = new Transaction(txId, coordinatorId, segmentIds, transactionType);
            transaction.setParticipants(participantsStatus);
        } else {
            transaction = new Transaction(txId, coordinatorId, segmentIds, transactionType);
        }

        try {
            if(coordinatorId.equals(peerProperties.selfId()) && participantsStatus.containsKey(peerProperties.selfId())){
                transaction.updateParticipantStatus(peerProperties.selfId(), TransactionStatus.PREPARED);
            }
            transactionRepository.save(transaction);
            holdManager.hold(segmentIds);
        } catch (IOException e) {
            throw new PersistenceErrorException("Falha ao salvar a transação.", e);
        }

        log.info("Transaction created: {}\nDecision status: {}", transaction, transaction.getDecision());
        return transaction;
    }

    public void resolveExpired(UUID txId, TransactionType transactionType) {
        Transaction t = transactionRepository.findById(txId);
        if (t == null || t.getCurrentStatus() != TransactionStatus.PREPARED) return;

        log.info("Resolving expired transaction: {}", txId);

        TransactionDecision decision = peerClient.askStatus(t.getCoordinatorId(), txId);

        if (decision == null || decision == TransactionDecision.PREPARE) {
            scheduler.schedule(() -> resolveExpired(txId, transactionType), 10, TimeUnit.SECONDS);
        } else if (decision == TransactionDecision.COMMIT) {
            commit(txId, transactionType);
        } else {
            abort(txId);
        }
    }
}
