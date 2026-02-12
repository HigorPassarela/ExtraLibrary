package br.com.ExtraLibrary.ExtraLibrary.dto.response;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookGender;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String isbn,
        String title,
        LocalDate publicationDate,
        BookGender gender,
        BigDecimal price,
        BookStatus bookStatus
) {
}
