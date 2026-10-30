package me.gimenez.server.exceptions;

public class TransactionRejectedException extends RuntimeException {
    public TransactionRejectedException(String message) {
        super(message);
    }
}
