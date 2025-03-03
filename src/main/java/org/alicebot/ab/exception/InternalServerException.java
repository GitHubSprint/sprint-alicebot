package org.alicebot.ab.exception;

public class InternalServerException extends RuntimeException{
    public InternalServerException(String errorMessage) {
        super(errorMessage);
    }
}
