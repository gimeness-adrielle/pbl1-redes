package me.gimenez.domain.models;

public enum TransactionStatus {
    PREPARED,
    PENDING,
    COMMITED,
    ROLL_BACK,
    ABORT,
    FINISHED
}
