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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/customer")
@Tag(name = "Customer Manager", description = "Manager for register Customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create/Register new Customer", description = "Endpoint for register customers")
    public ResponseEntity<Object> save(@RequestBody @Valid CustomerRequest customerRequest) {
        try {
            Customer customer = CustomerMapper.toEntity(customerRequest);
            service.save(customer);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(customer.getId())
                    .toUri();

            return ResponseEntity.created(location).build();

        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Endpoint for update customers details")
    public ResponseEntity<Object> update(@Parameter(description = "Customer ID", required = true) @PathVariable("id") String id, @RequestBody CustomerRequest customerRequest) {
        try {
            UUID customerId = UUID.fromString(id);
            Customer customer = CustomerMapper.toEntity(customerRequest);
            service.update(customerId,customer);

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

    @GetMapping("/{id}")
    @Operation(summary = "Get details from id", description = "Endpoint for Get customers details from id")
    public ResponseEntity<CustomerResponse> getDetailsFromId(@PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);

            return service
                    .getForId(customerId)
                    .map(customer -> {
                        CustomerResponse customerResponse = CustomerMapper.toDTO(customer);
                        return ResponseEntity.ok(customerResponse);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "List all customers", description = "Endpoint for list all customers")
    public ResponseEntity<List<CustomerResponse>> getAll() {
        List<Customer> customers = service.getAll();
        List<CustomerResponse> customerResponses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(customerResponses);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get customer by name", description = "Find customer by your name")
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

    @GetMapping("/email/{email}")
    @Operation(summary = "Get by email", description = "Endpoint for find customer by email")
    public ResponseEntity<CustomerResponse> findByEmail(@Parameter(description = "Email from customer", required = true) @PathVariable("email") String email) {
        return service
                .findByEmail(email)
                .map(customer -> {
                    CustomerResponse customerResponse = CustomerMapper.toDTO(customer);
                    return ResponseEntity.ok(customerResponse);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cpf/{cpf}")
    @Operation(summary = "Get by cpf", description = "Endpoint for find customer by cpf")
    public ResponseEntity<CustomerResponse> findByCpf(@Parameter(description = "Cpf from customer", required = true) @PathVariable("cpf") String cpf) {
        return service
                .findByCpf(cpf)
                .map(customer -> {
                    CustomerResponse customerResponse = CustomerMapper.toDTO(customer);
                    return ResponseEntity.ok(customerResponse);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get by status", description = "Endpoint for find customer by status")
    public ResponseEntity<Object> getCustomerByStatus(@Parameter(description = "Status from customer (ACTIVE, BLOCKED, DISABLED)", required = true) @PathVariable("status") String status) {
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

    @GetMapping("/active")
    @Operation(summary = "Get Active status", description = "Endpoint for find customer with Active status")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        List<Customer> customers = service.getActiveCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get Blocked status", description = "Endpoint for find customer with Blocked status")
    public ResponseEntity<List<CustomerResponse>> getBlockedCustomer() {
        List<Customer> customers = service.getBlockedCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/disabled")
    @Operation(summary = "Get Disabled status", description = "Endpoint for find customer with Disabled status")
    public ResponseEntity<List<CustomerResponse>> getDisabledCustomer() {
        List<Customer> customers = service.getDisabledCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate customer", description = "Endpoint for Activate customer status")
    public ResponseEntity<Object> activateCustomer(@Parameter(description = "Customer ID", required = true) @PathVariable("id") String id){
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
    @Operation(summary = "Block customer", description = "Endpoint for Block customer status")
    public ResponseEntity<Object> blockCustomer(@Parameter(description = "Customer ID", required = true) @PathVariable("id") String id) {
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
    @Operation(summary = "Disable customer", description = "Endpoint for Disable customer status")
    public ResponseEntity<Object> disableCustomer(@Parameter(description = "Customer ID", required = true) @PathVariable("id") String id) {
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
    @Operation(summary = "Delete customer", description = "Endpoint for delete customer")
    public ResponseEntity<Object> delete(@Parameter(description = "Customer ID", required = true) @PathVariable("id") String id) {
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
