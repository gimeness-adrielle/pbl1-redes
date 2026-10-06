package me.gimenez.exceptions;

public class CommunicationErrorException extends RuntimeException {
    public CommunicationErrorException(String message) {
        super(message);
    }
}
