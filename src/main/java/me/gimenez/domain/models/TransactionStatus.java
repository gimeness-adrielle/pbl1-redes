package me.gimenez.domain.models;

public enum TransactionStatus {
    PREPARING,
    PREPARED,
    COMMITTING,
    COMMITTED,
    ABORTED,
}
