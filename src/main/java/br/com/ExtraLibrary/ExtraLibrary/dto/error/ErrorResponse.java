package br.com.ExtraLibrary.ExtraLibrary.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp,
        List<ErrorField> validationErrors
) {

    // Construtor para erros simples (sem validation errors)
    public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
        this(status, error, message, timestamp, null);
    }

    // Métodos estáticos para erros simples
    public static ErrorResponse badrequest(String message) {
        return new ErrorResponse(400, "Bad Request", message, LocalDateTime.now(), null);
    }

    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse(401, "Unauthorized", message, LocalDateTime.now(), null);
    }

    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse(403, "Forbidden", message, LocalDateTime.now(), null);
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "Not Found", message, LocalDateTime.now(), null);
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "Conflict", message, LocalDateTime.now(), null);
    }

    public static ErrorResponse internalServerError(String message) {
        return new ErrorResponse(500, "Internal Server Error", message, LocalDateTime.now(), null);
    }

    // Métodos estáticos para erros com validation
    public static ErrorResponse validationError(String message, List<ErrorField> validationErrors) {
        return new ErrorResponse(400, "Validation Error", message, LocalDateTime.now(), validationErrors);
    }

    public static ErrorResponse badRequestWithValidation(String message, List<ErrorField> validationErrors) {
        return new ErrorResponse(400, "Bad Request", message, LocalDateTime.now(), validationErrors);
    }
}