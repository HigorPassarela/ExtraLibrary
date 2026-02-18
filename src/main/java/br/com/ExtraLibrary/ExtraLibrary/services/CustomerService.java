package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.CustomerValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerValidator validator;

    public CustomerService(CustomerRepository repository, CustomerValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public Customer save(Customer customer) {
        if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }

        validator.valid(customer);
        return repository.save(customer);
    }

    @Transactional
    public Customer update(UUID id, Customer customerUpdated) {
        Customer customerExisting = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customerUpdated.setId(id);

        if (customerUpdated.getStatus() == null) {
            customerUpdated.setStatus(customerExisting.getStatus());
        }

        validator.valid(customerUpdated);

        return repository.save(customerUpdated);
    }

    @Transactional
    public Optional<Customer> getForId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public List<Customer> getAll() {
        return repository.findAll();
    }

    @Transactional
    public List<Customer> findByName(String name) {
        return repository.findByName(name);
    }

    @Transactional
    public Optional<Customer> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Transactional
    public Optional<Customer> findByCpf(String cpf) {
        return repository.findByCpf(cpf);
    }

    @Transactional
    public List<Customer> getCustomerByStatus(CustomerStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional
    public List<Customer> getActiveCustomers() {
        return repository.findByStatus(CustomerStatus.ACTIVE);
    }

    @Transactional
    public List<Customer> getBlockedCustomers() {
        return repository.findByStatus(CustomerStatus.BLOCKED);
    }

    @Transactional
    public List<Customer> getDisabledCustomers() {
        return repository.findByStatus(CustomerStatus.DISABLED);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        repository.delete(customer);
    }

    @Transactional
    public Customer activateCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.ACTIVE);
        return repository.save(customer);
    }

    @Transactional
    public Customer blockedCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.BLOCKED);
        return repository.save(customer);
    }

    @Transactional
    public Customer disabledCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.DISABLED);
        return repository.save(customer);
    }
}
