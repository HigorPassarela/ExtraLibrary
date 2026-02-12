package br.com.ExtraLibrary.ExtraLibrary.exception;

public class DuplicatedRegisterException extends RuntimeException{
    public DuplicatedRegisterException(String message) {
        super(message);
    }
}
