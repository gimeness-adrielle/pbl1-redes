package me.gimenez.server.exceptions;

public class ReservationNotCreatedException extends RuntimeException {
    public ReservationNotCreatedException(String message) {
        super(message);
    }
}
