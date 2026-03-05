package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.CustomerRequest;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.CustomerMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerRole;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.CustomerValidator;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerValidator validator;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository repository, CustomerValidator validator, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Método auxiliar para verificar propriedade
    public boolean isOwner(UUID customerId, String email) {
        return repository.findById(customerId)
                .map(customer -> customer.getEmail().equals(email))
                .orElse(false);
    }

    // ✅ Método auxiliar para verificar roles
    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    @Transactional
    public Customer save(Customer customer) {
        if (customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }
        if (customer.getRole() == null) {
            customer.setRole(CustomerRole.CUSTOMER);
        }

        // Se a senha não estiver criptografada, criptografar
        if (customer.getPassword() != null && !customer.getPassword().startsWith("$2a$")) {
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        }

        validator.valid(customer);
        return repository.save(customer);
    }

    // ✅ Update com autorização contextual
    @Transactional
    public Customer update(UUID id, Customer customerUpdated, Authentication authentication) {
        Customer customerExisting = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        // ✅ Verificar se CUSTOMER está tentando editar outro perfil
        if (hasRole(authentication, "CUSTOMER")) {
            if (!customerExisting.getEmail().equals(authentication.getName())) {
                throw new AccessDeniedException("Você só pode editar seu próprio perfil");
            }
            // CUSTOMER não pode alterar role nem status
            customerUpdated.setRole(customerExisting.getRole());
            customerUpdated.setStatus(customerExisting.getStatus());
        }

        customerUpdated.setId(id);

        if (customerUpdated.getStatus() == null) {
            customerUpdated.setStatus(customerExisting.getStatus());
        }
        if (customerUpdated.getRole() == null) {
            customerUpdated.setRole(customerExisting.getRole());
        }

        // Se uma nova senha foi fornecida, criptografar
        if (customerUpdated.getPassword() != null && !customerUpdated.getPassword().startsWith("$2a$")) {
            customerUpdated.setPassword(passwordEncoder.encode(customerUpdated.getPassword()));
        } else if (customerUpdated.getPassword() == null) {
            customerUpdated.setPassword(customerExisting.getPassword());
        }

        validator.valid(customerUpdated);
        return repository.save(customerUpdated);
    }

    @Transactional
    public Optional<Customer> getForId(UUID id) {
        return repository.findById(id);
    }

    // ✅ Apenas ADMIN/LIBRARIAN podem listar todos
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> getAll() {
        return repository.findAll();
    }

    // ✅ Apenas ADMIN/LIBRARIAN podem buscar por nome
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> findByName(String name) {
        return repository.findByName(name);
    }

    // ✅ Apenas ADMIN/LIBRARIAN podem buscar por email
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Optional<Customer> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    // ✅ Apenas ADMIN/LIBRARIAN podem buscar por CPF
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Optional<Customer> findByCpf(String cpf) {
        return repository.findByCpf(cpf);
    }

    // ✅ Apenas ADMIN/LIBRARIAN podem filtrar por status
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> getCustomerByStatus(CustomerStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> getActiveCustomers() {
        return repository.findByStatus(CustomerStatus.ACTIVE);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> getBlockedCustomers() {
        return repository.findByStatus(CustomerStatus.BLOCKED);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Customer> getDisabledCustomers() {
        return repository.findByStatus(CustomerStatus.DISABLED);
    }

    // ✅ Apenas ADMIN pode deletar
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        repository.delete(customer);
    }

    // ✅ Apenas ADMIN pode alterar status
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Customer activateCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.ACTIVE);
        return repository.save(customer);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Customer blockedCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.BLOCKED);
        return repository.save(customer);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Customer disabledCustomer(UUID id) {
        Customer customer = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente com id: " + id + " não encontrado!"));

        customer.setStatus(CustomerStatus.DISABLED);
        return repository.save(customer);
    }

    // ✅ Novo: Obter próprio perfil
    @Transactional
    public Customer getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));
    }

    // ✅ Novo: Atualizar próprio perfil
    @Transactional
    public Customer updateMyProfile(CustomerRequest request, Authentication authentication) {
        String email = authentication.getName();
        Customer currentProfile = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil não encontrado"));

        // Mapear apenas campos editáveis
        currentProfile.setName(request.name());
        currentProfile.setPhone(request.phone());
        currentProfile.setAddress(request.address());

        // Se forneceu nova senha, criptografar
        if (request.password() != null && !request.password().isEmpty()) {
            currentProfile.setPassword(passwordEncoder.encode(request.password()));
        }

        // Manter role e status originais (usuário não pode alterar)
        validator.valid(currentProfile);
        return repository.save(currentProfile);
    }
}