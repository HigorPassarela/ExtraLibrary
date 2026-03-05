package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.CustomerRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.CustomerResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.CustomerMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/customer")
@Tag(name = "Customer Manager", description = "Manager for register Customers")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService service;
    private final PasswordEncoder passwordEncoder;

    public CustomerController(CustomerService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Criação de customer (público via auth/register, aqui apenas para ADMIN/LIBRARIAN)
    @PostMapping
    @Operation(summary = "Create Customer", description = "Create customer - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> save(@RequestBody @Valid CustomerRequest customerRequest) {
        try {
            Customer customer = CustomerMapper.toEntity(customerRequest, passwordEncoder);
            Customer saved = service.save(customer);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).build();

        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Atualização contextual - user pode editar próprio perfil
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update customer details")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('CUSTOMER') and @customerService.isOwner(#id, authentication.name))")
    public ResponseEntity<Object> update(
            @Parameter(description = "Customer ID", required = true) @PathVariable("id") String id,
            @RequestBody @Valid CustomerRequest customerRequest,
            Authentication authentication) {
        try {
            UUID customerId = UUID.fromString(id);
            Customer customer = CustomerMapper.toEntity(customerRequest, passwordEncoder);
            service.update(customerId, customer, authentication);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Visualização contextual
    @GetMapping("/{id}")
    @Operation(summary = "Get customer details", description = "Get customer details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('CUSTOMER') and @customerService.isOwner(#id, authentication.name))")
    public ResponseEntity<CustomerResponse> getDetailsFromId(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            return service.getForId(customerId)
                    .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Listar todos - apenas ADMIN/LIBRARIAN
    @GetMapping
    @Operation(summary = "List all customers", description = "List all customers - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getAll() {
        List<Customer> customers = service.getAll();
        List<CustomerResponse> customerResponses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(customerResponses);
    }

    // ✅ Buscar por nome - apenas ADMIN/LIBRARIAN
    @GetMapping("/name/{name}")
    @Operation(summary = "Get customer by name", description = "Find customer by name - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getByName(@PathVariable("name") String name) {
        try {
            List<Customer> customers = service.findByName(name);
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest("Nome Inválido");
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound("Nome: " + name + " não encontrado!");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Buscar por email - apenas ADMIN/LIBRARIAN
    @GetMapping("/email/{email}")
    @Operation(summary = "Get by email", description = "Find customer by email - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> findByEmail(@PathVariable("email") String email) {
        return service.findByEmail(email)
                .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Buscar por CPF - apenas ADMIN/LIBRARIAN
    @GetMapping("/cpf/{cpf}")
    @Operation(summary = "Get by cpf", description = "Find customer by CPF - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> findByCpf(@PathVariable("cpf") String cpf) {
        return service.findByCpf(cpf)
                .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Filtrar por status - apenas ADMIN/LIBRARIAN
    @GetMapping("/status/{status}")
    @Operation(summary = "Get by status", description = "Find customers by status - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getCustomerByStatus(@PathVariable("status") String status) {
        try {
            CustomerStatus customerStatus = CustomerStatus.valueOf(status.toUpperCase());
            List<Customer> customers = service.getCustomerByStatus(customerStatus);
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Endpoints de status específicos - apenas ADMIN/LIBRARIAN
    @GetMapping("/active")
    @Operation(summary = "Get active customers", description = "Get all active customers - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        List<Customer> customers = service.getActiveCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get blocked customers", description = "Get all blocked customers - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getBlockedCustomer() {
        List<Customer> customers = service.getBlockedCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/disabled")
    @Operation(summary = "Get disabled customers", description = "Get all disabled customers - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getDisabledCustomer() {
        List<Customer> customers = service.getDisabledCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // ✅ Endpoint para próprio perfil
    @GetMapping("/profile")
    @Operation(summary = "Get my profile", description = "Get current user profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        Customer customer = service.getMyProfile(authentication);
        return ResponseEntity.ok(CustomerMapper.toDTO(customer));
    }

    // ✅ Atualizar próprio perfil
    @PutMapping("/profile")
    @Operation(summary = "Update my profile", description = "Update current user profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> updateMyProfile(
            @RequestBody @Valid CustomerRequest customerRequest,
            Authentication authentication) {
        try {
            service.updateMyProfile(customerRequest, authentication);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            var error = ErrorResponse.badrequest("Erro ao atualizar perfil: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Gestão de status - apenas ADMIN (já configurado no SecurityConfig)
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate customer", description = "Activate customer - ADMIN only")
    public ResponseEntity<Object> activateCustomer(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            service.activateCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Block customer", description = "Block customer - ADMIN only")
    public ResponseEntity<Object> blockCustomer(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            service.blockedCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable customer", description = "Disable customer - ADMIN only")
    public ResponseEntity<Object> disableCustomer(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            service.disabledCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Delete customer - ADMIN only")
    public ResponseEntity<Object> delete(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            service.delete(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}