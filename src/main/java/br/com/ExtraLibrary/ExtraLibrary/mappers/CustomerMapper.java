package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.CustomerRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.CustomerResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;

public class CustomerMapper {

    public static Customer toEntity(CustomerRequest customerRequest) {
        return Customer.builder()
                .name(customerRequest.name())
                .email(customerRequest.email())
                .cpf(customerRequest.cpf())
                .phone(customerRequest.phone())
                .dateBirth(customerRequest.dateBirth())
                .address(customerRequest.address())
                .build();
    }

    public static CustomerResponse toDTO (Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCpf(),
                customer.getPhone(),
                customer.getDateBirth(),
                customer.getAddress(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
