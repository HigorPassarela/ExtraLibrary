package br.com.ExtraLibrary.ExtraLibrary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record CustomerRequest(
        @NotBlank(message = "Campo name Obrigatório!")
        String name,
        @Email
        @NotBlank(message = "Campo email Obrigatório!")
        String email,
        @CPF
        @NotBlank(message = "Campo cpf Obrigatório!")
        String cpf,
        @NotBlank(message = "Campo phone Obrigatório!")
        String phone,
        @NotNull(message = "Campo dateBirth Obrigatório!")
        LocalDate dateBirth,
        @NotBlank(message = "Campo address Obrigatório!")
        String address
) {
}
