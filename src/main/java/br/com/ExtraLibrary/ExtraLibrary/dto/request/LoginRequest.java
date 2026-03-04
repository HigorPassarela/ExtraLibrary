package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email deve ser válido!")
        @NotBlank(message = "Email é obrigatório!")
        String email,

        @NotBlank(message = "Senha é obrigatória!")
        String password
) {
}
