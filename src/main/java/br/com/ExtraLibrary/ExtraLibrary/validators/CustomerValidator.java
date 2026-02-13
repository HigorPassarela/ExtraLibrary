package br.com.ExtraLibrary.ExtraLibrary.validators;

import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerValidator {

    private CustomerRepository repository;

    public CustomerValidator(CustomerRepository repository) {
        this.repository = repository;
    }

    public void valid(Customer customer) {
        validateEmailUniqueness(customer);
        validateCpfUniqueness(customer);
    }

    private void validateEmailUniqueness(Customer customer) {
        Optional<Customer> existingByEmail = repository.findByEmail(customer.getEmail());

        if (existingByEmail.isPresent()) {
            Customer existing = existingByEmail.get();

            if (customer.getId() == null || !customer.getId().equals(existing.getId())) {
                throw new DuplicatedRegisterException("Já existe um cliente com este email: " + customer.getEmail());
            }
        }
    }

    private void validateCpfUniqueness(Customer customer) {
        if (customer.getCpf() != null && !customer.getCpf().trim().isEmpty()) {
            Optional<Customer> existingByCpf = repository.findByCpf(customer.getCpf());

            if (existingByCpf.isPresent()) {
                Customer existing = existingByCpf.get();

                // Se é criação ou é um cliente diferente
                if (customer.getId() == null || !customer.getId().equals(existing.getId())) {
                    throw new DuplicatedRegisterException("Já existe um cliente com este CPF: " + customer.getCpf());
                }
            }
        }
    }
}
