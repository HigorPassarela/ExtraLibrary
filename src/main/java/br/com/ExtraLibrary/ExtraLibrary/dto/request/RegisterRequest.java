package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Campo name é obrigatório!")
        String name,

        @Email(message = "Email deve ser válido!")
        @NotBlank(message = "Campo email é obrigatório!")
        String email,

        @NotBlank(message = "Campo password é obrigatório!")
        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres!")
        String password,

        @CPF(message = "CPF deve ser válido!")
        String cpf, // Opcional no seu schema

        @NotBlank(message = "Campo phone é obrigatório!")
        String phone,

        @NotNull(message = "Campo dateBirth é obrigatório!")
        LocalDate dateBirth,

        @NotBlank(message = "Campo address é obrigatório!")
        String address
) {
}
