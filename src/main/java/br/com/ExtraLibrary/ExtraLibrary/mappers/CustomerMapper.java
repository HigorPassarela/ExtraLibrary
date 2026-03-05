package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.CustomerRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.CustomerResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerRole;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomerMapper {

    public static Customer toEntity(CustomerRequest customerRequest, PasswordEncoder passwordEncoder) {
        return Customer.builder()
                .name(customerRequest.name())
                .email(customerRequest.email())
                .password(passwordEncoder.encode(customerRequest.password()))
                .cpf(customerRequest.cpf())
                .phone(customerRequest.phone())
                .dateBirth(customerRequest.dateBirth())
                .address(customerRequest.address())
                .status(CustomerStatus.ACTIVE)
                .role(CustomerRole.CUSTOMER)
                .build();
    }

    public static CustomerResponse toDTO(Customer customer) {
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
