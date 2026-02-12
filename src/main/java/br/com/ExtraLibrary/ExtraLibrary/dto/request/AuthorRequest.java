package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AuthorRequest(
        @NotBlank(message = "Campo name é obrigatório!")
        String name,
        String biography,
        @NotBlank(message = "Campo nacionality é obrigatório!")
        String nacionality,
        @NotNull(message = "Campo birthDate é obrigatório!")
        LocalDate birthDate
) {
}
