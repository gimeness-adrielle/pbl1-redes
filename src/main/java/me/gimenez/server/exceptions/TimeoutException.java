package me.gimenez.server.exceptions;

public class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
        super(message);
    }
}
