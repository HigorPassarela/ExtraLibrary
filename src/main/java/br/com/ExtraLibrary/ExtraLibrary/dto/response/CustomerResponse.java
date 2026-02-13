package br.com.ExtraLibrary.ExtraLibrary.dto.response;

import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        LocalDate dateBirth,
        String address,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
