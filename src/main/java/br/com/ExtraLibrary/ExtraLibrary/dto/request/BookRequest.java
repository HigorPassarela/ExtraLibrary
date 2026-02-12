package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookGender;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.ISBN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookRequest(
        @ISBN
        @NotBlank(message = "Campo isbn Obrigatório!")
        String isbn,
        @NotBlank(message = "Campo title Obrigatório!")
        String title,
        @NotNull(message = "Campo publicationDate Obrigatório!")
        LocalDate publicationDate,
        @NotNull(message = "Campo gender Obrigatório!")
        BookGender gender,
        @NotNull(message = "Campo price Obrigatório!")
        BigDecimal price,
        @NotNull(message = "Campo bookStatus Obrigatório!")
        BookStatus bookStatus,
        @NotNull(message = "Campo authorId é obrigatório!")
        UUID authorId
) {
}
