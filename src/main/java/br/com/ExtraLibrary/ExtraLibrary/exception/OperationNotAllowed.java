package br.com.ExtraLibrary.ExtraLibrary.exception;

public class OperationNotAllowed extends RuntimeException{
    public OperationNotAllowed(String message) {
        super(message);
    }
}
