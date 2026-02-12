package br.com.ExtraLibrary.ExtraLibrary.dto.error;

import org.springframework.http.HttpStatus;

import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<ErrorField> errors
) {

    public static ErrorResponse defaultResponse(String mensagem) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), mensagem, List.of());
    }

    public static ErrorResponse conflict(String mensagem) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), mensagem, List.of());
    }

    public static ErrorResponse notFound(String mensagem) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), mensagem, List.of());
    }
}
