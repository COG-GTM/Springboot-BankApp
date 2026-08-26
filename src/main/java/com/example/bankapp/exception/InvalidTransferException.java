package com.example.bankapp.exception;

/**
 * Raised when a transfer request is rejected before any balance is modified.
 */
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
