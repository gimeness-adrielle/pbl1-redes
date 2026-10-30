package me.gimenez.util;

import me.gimenez.server.exceptions.PersistenceErrorException;
import me.gimenez.server.exceptions.TransactionNotFoundException;
import me.gimenez.server.exceptions.TransactionRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<String> handleNotFound(
            TransactionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }

    @ExceptionHandler(TransactionRejectedException.class)
    public ResponseEntity<String> handleRejected(
            TransactionRejectedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }

    @ExceptionHandler(PersistenceErrorException.class)
    public ResponseEntity<String> handlePersistenceError(
            PersistenceErrorException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }
}
