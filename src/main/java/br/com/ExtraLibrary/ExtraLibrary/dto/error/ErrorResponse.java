package br.com.ExtraLibrary.ExtraLibrary.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {

    public static ErrorResponse badrequest(String message) {
        return new ErrorResponse(400, "Bad Request", message, LocalDateTime.now());
    }

    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse(401, "Unauthorized", message, LocalDateTime.now());
    }

    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse(403, "Forbidden", message, LocalDateTime.now());
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "Not Found", message, LocalDateTime.now());
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "Conflict", message, LocalDateTime.now());
    }

    public static ErrorResponse internalServerError(String message) {
        return new ErrorResponse(500, "Internal Server Error", message, LocalDateTime.now());
    }
}