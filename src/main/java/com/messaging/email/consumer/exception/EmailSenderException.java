package com.messaging.email.consumer.exception;

public class EmailSenderException extends Exception {
    public EmailSenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSenderException(String message) {
        super(message);
    }
}
