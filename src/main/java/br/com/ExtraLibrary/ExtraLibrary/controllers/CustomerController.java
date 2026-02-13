package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.CustomerRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.CustomerResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.CustomerMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get details from id", description = "Endpoint for Get customers details from id")
    public ResponseEntity<CustomerResponse> getDetailsFromId(@PathVariable("id") String id) {
        try{
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
}
