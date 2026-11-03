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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final PeerClient peerClient;
    private final PeerProperties peerProperties;
    private final ScheduledExecutorService scheduler;
    private final TransactionRepository transactionRepository;
    private final RideRepository rideRepository;
    private final LockService lockService;
    private final HoldManager holdManager;

    private final PriorityQueue<Transaction> competitors = new PriorityQueue<>();

    /**
     * This function checks whether all specified segments are available for booking. If at least one segment is unavailable,
     * it returns false. It uses the {@linkplain #remainingSeats(Segment)} method, which returns the number of remaining
     * seats, calculated as the currently available seats minus the number of active holds temporary bookings.
     * @param segments list of segments to be checked.
     * @return true if all specified segments are available, otherwise false.
     */
    public boolean checkAtomicity(List<Segment> segments) {
        return segments.stream().allMatch(segment -> remainingSeats(segment) > 0);
    }

    /**
     * Returns the number of remaining seats, calculated as the currently available seats minus the number of active
     * holds temporary bookings.
     * @param segment the segment to be calculated.
     * @return number of remaining seats.
     */
    private int remainingSeats(Segment segment) {
        return segment.getAvailableSeats() - holdManager.getHolds(segment.getId());
    }

    /** Initial prepare phase, build the transaction and checks if the booking it can be contested or not.
     * @param transaction the transaction to be prepared.
     * @return the status of the preparation operation.
     */
    public TransactionStatus prepare(Transaction transaction) {
        Transaction tx = buildTransaction(transaction);

        if (isContested(tx)) {
            return prepareByPrecedence(tx);
        }
        return doPrepare(tx);
    }

    /** Checks whether a transaction is being contested. The first criterion is whether the transaction type is "CANCEL".
     * If it is, the method returns false immediately, as "CANCEL"-type operations on transactions cannot be contested.
     * Next, it checks whether at least one segment of the transaction has exactly one seat available. This is done using
     * the {@linkplain #remainingSeats(Segment)} function.
     * @param tx the transaction to be checked.
     * @return true if the transaction contains at least one segment with only one seat available. Otherwise, returns false.
     * It may also return false if the transaction type is "CANCEL".
     */
    private boolean isContested(Transaction tx) {
        if (tx.getType() != TransactionType.CREATE_RESERVATION) return false;

        List<Segment> segments = rideRepository.findSegmentsById(tx.getSegmentIds());

        return segments != null && segments.stream()
                .anyMatch(s -> remainingSeats(s) == 1);
    }

    /** This function uses a priority queue {@linkplain #competitors} to orchestrate transactions from competing participants.
     * It employs a time window to allow participants to arrive, and uses the {@linkplain #wait()} method to compel a
     * participant to await its turn. The {@linkplain #doPrepare(Transaction)} method is invoked to prepare the transaction,
     * following the order defined by the priority queue. The priority queue utilizes the "compareTo" method implementation on
     * {@link Transaction}, to order elements based on the lowest logical timestamp, using the server ID as a tie-breaker.
     * Once the preparation operation is complete, it is removed from the queue, and the remaining threads are notified via
     * {@linkplain #notifyAll()}.
     * @param tx transaction to be prepared.
     * @return the status of the preparation operation.
     */
    private TransactionStatus prepareByPrecedence(Transaction tx) {
        synchronized (competitors) {
            competitors.add(tx);
        }

        try {
            long windowMs = 300; // Time window
            Thread.sleep(windowMs);
            waitForTurn(tx);
            return doPrepare(tx);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionRejectedException("Pedido interrompido.");
        } finally {
            synchronized (competitors) {
                competitors.remove(tx);
                competitors.notifyAll();
            }
        }
    }

    /** Checks if the transaction is the first of the priority queue {@linkplain #competitors}. If it's not, it makes the
     * thread wait with {@linkplain #wait()}.
     * @param tx Transaction
     * @throws InterruptedException threw by {@linkplain #wait()}.
     */
    private void waitForTurn(Transaction tx) throws InterruptedException {
        synchronized (competitors) {
            while (competitors.peek() != tx) {
                competitors.wait();
            }
        }
    }


    private Transaction buildTransaction(Transaction tx) {
        // Marks each transaction participant with the status "PREPARING".
        if (!tx.getParticipants().isEmpty()) {
            tx.getParticipants().forEach((id, _) ->
                    tx.updateParticipantStatus(id, TransactionStatus.PREPARING));
        }
        // If the server is the transaction coordinator, it marks itself as "PREPARED".
        if (tx.getCoordinatorId().equals(peerProperties.selfId())
                && tx.getParticipants().containsKey(peerProperties.selfId())) {
            tx.updateParticipantStatus(peerProperties.selfId(), TransactionStatus.PREPARED);
        }
        return tx;
    }

    private TransactionStatus doPrepare(Transaction tx) {
        UUID txId = tx.getTxId();
        List<UUID> segmentIds = tx.getSegmentIds();
        log.info("{} PREPARING: preparing transaction {}", peerProperties.selfId().toUpperCase(), txId);

        List<ReentrantLock> locks = lockService.lock(segmentIds);
        try {
            Transaction existing = transactionRepository.findById(txId);
            if (existing != null) {
                log.info("Transaction {} already exists.", txId);
                return existing.getCurrentStatus();
            }

            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null) {
                throw new TransactionRejectedException("Um ou mais segmentos não foram encontrados.");
            }
            if (tx.getType() == TransactionType.CREATE_RESERVATION && !checkAtomicity(currentSegments)) {
                throw new TransactionRejectedException("Não há assentos disponíveis para todos os segmentos.");
            }

            // Records the transaction and holds the seats
            try {
                transactionRepository.save(tx);
            } catch (IOException e) {
                throw new PersistenceErrorException("Falha ao salvar a transação.", e);
            }
            holdManager.hold(tx.getSegmentIds());

            if (!tx.getCoordinatorId().equals(peerProperties.selfId())) {
                scheduler.schedule(() -> resolveExpired(txId), 15, TimeUnit.SECONDS);
            }
            return TransactionStatus.PREPARED;
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
    }

    public Transaction commit (UUID txId){
        Transaction transaction = transactionRepository.findById(txId);
        if (transaction == null){
            throw new TransactionNotFoundException("Não foi encontrada uma transação com este id.");
        }

        log.info("{} COMMITING: transaction {}\nStatus: {}", peerProperties.selfId().toUpperCase(), txId, transaction.getCurrentStatus());

        List<UUID> segmentIds = transaction.getSegmentIds();

        List<ReentrantLock> locks = lockService.lock(segmentIds);

        try {
            if (transaction.getCurrentStatus() != TransactionStatus.PREPARED
                    && transaction.getCurrentStatus() != TransactionStatus.COMMITTING) {
                log.info("This transaction has already been {}.", transaction.getCurrentStatus().toString().toLowerCase());
                return transaction;
            }

            transaction.setDecision(TransactionDecision.COMMIT);
            transaction.setCurrentStatus(TransactionStatus.COMMITTING);
            try{
                transactionRepository.save(transaction);
            } catch (IOException e) {
                throw new PersistenceErrorException("Falha ao persistir o commit da transação.", e);
            }

            List<Segment> currentSegments = rideRepository.findSegmentsById(segmentIds);
            if (currentSegments == null) {
                throw new TransactionRejectedException("Um ou mais segmentos não foram encontrados.");
            }

            if (transaction.getType() == TransactionType.CREATE_RESERVATION){
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
            transaction.setDecision(TransactionDecision.ABORT);

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

    public void resolveExpired(UUID txId) {
        Transaction t = transactionRepository.findById(txId);
        if (t == null || t.getCurrentStatus() != TransactionStatus.PREPARED) return;

        log.info("Resolving expired transaction: {}", txId);

        TransactionDecision decision = peerClient.askStatus(t.getCoordinatorId(), txId).decision();

        if (decision == null || decision == TransactionDecision.PREPARE) {
            scheduler.schedule(() -> resolveExpired(txId), 10, TimeUnit.SECONDS);
        } else if (decision == TransactionDecision.COMMIT) {
            commit(txId);
        } else {
            abort(txId);
        }
    }
}
