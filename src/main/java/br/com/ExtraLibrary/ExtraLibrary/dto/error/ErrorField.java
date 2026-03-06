package br.com.ExtraLibrary.ExtraLibrary.dto.error;

public record ErrorField(
        String field,
        Object rejectedValue,
        String message
) {}
