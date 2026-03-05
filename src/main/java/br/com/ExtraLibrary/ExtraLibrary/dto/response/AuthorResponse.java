package br.com.ExtraLibrary.ExtraLibrary.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AuthorResponse(
        UUID id,
        String name,
        String biography,
        String nacionality,
        LocalDate birthDate,
        int totalBooks,
        List<BookResponse> books // ✅ Incluir lista de livros se necessário
) {}
