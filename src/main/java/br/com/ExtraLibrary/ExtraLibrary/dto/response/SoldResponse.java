package br.com.ExtraLibrary.ExtraLibrary.dto.response;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SoldResponse(
        Long id,
        UUID customerId,
        String customerName,
        String customerEmail,
        List<BookSummary> books,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal finalPrice,
        FormPayment formPayment,
        LocalDateTime dateSale,
        LocalDateTime createdAt
) {
    public static record BookSummary(
            UUID id,
            String title,
            String author,
            BigDecimal price
    ) {}
}
