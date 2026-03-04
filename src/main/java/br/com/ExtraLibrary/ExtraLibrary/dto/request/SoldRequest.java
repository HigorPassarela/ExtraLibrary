package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SoldRequest(
        @NotNull(message = "Campo customerId é obrigatório")
        UUID customerId,
        @NotEmpty(message = "List de bookIds não pode estar vazia")
        Map<UUID, Long> booksQuantity,
        @DecimalMin(value = "0.0", inclusive = true, message = "Desconto não pode ser negativo")
        BigDecimal discount,
        @NotNull(message = "Campo FormPayment é obrigatório")
        FormPayment formPayment
) {
}
