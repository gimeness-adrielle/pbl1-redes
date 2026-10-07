package me.gimenez.server.exceptions;

public class CommunicationErrorException extends RuntimeException {
    public CommunicationErrorException(String message) {
        super(message);
    }
}
