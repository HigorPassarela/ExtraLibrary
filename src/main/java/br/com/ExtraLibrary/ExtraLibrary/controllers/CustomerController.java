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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Customer Management", description = "Gerenciamento de Clientes - Perfis, Status e Controle Contextual")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService service;
    private final PasswordEncoder passwordEncoder;

    public CustomerController(CustomerService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @Operation(
            summary = "Criar Cliente (Administrativo)",
            description = "Cria um cliente diretamente pelo painel administrativo. " +
                    "Nota: Clientes normalmente se registram via /auth/register."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "409", description = "Email ou CPF já cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar Cliente",
            description = "Atualiza dados do cliente. Controle Contextual: " +
                    "ADMIN/LIBRARIAN pode editar qualquer cliente. " +
                    "CUSTOMER só pode editar próprio perfil."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Não pode editar este perfil"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "409", description = "Email ou CPF já em uso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('CUSTOMER') and @customerService.isOwner(#id, authentication.name))")
    public ResponseEntity<Object> update(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
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

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar Cliente por ID",
            description = "Retorna dados do cliente. Controle Contextual: " +
                    "ADMIN/LIBRARIAN tem acesso total a qualquer cliente. " +
                    "CUSTOMER só visualiza próprio perfil."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do cliente retornados"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Não pode ver este perfil"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or (hasRole('CUSTOMER') and @customerService.isOwner(#id, authentication.name))")
    public ResponseEntity<CustomerResponse> getDetailsFromId(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
        try {
            UUID customerId = UUID.fromString(id);
            return service.getForId(customerId)
                    .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(
            summary = "Listar Todos os Clientes",
            description = "Retorna lista completa de clientes cadastrados. " +
                    "Informações administrativas para gestão da base de clientes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getAll() {
        List<Customer> customers = service.getAll();
        List<CustomerResponse> customerResponses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(customerResponses);
    }

    @GetMapping("/name/{name}")
    @Operation(
            summary = "Buscar por Nome",
            description = "Busca clientes por nome (busca parcial). " +
                    "Ferramenta administrativa para localizar clientes específicos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca por nome realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Nenhum cliente encontrado com este nome"),
            @ApiResponse(responseCode = "400", description = "Nome inválido")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getByName(
            @Parameter(description = "Nome ou parte do nome", required = true, example = "João Silva")
            @PathVariable("name") String name) {
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
    @Operation(
            summary = "Buscar por Email",
            description = "Localiza cliente pelo endereço de email. " +
                    "Útil para suporte ao cliente e verificações administrativas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado pelo email"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Email não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> findByEmail(
            @Parameter(description = "Endereço de email", required = true, example = "cliente@email.com")
            @PathVariable("email") String email) {
        return service.findByEmail(email)
                .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cpf/{cpf}")
    @Operation(
            summary = "Buscar por CPF",
            description = "Localiza cliente pelo número do CPF. " +
                    "Identificação única para processos administrativos e fiscais."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado pelo CPF"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "CPF não encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> findByCpf(
            @Parameter(description = "Número do CPF", required = true, example = "12345678901")
            @PathVariable("cpf") String cpf) {
        return service.findByCpf(cpf)
                .map(customer -> ResponseEntity.ok(CustomerMapper.toDTO(customer)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(
            summary = "Filtrar por Status",
            description = "Filtra clientes por status atual. " +
                    "Status disponíveis: ACTIVE, BLOCKED, DISABLED"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes filtrados por status"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "400", description = "Status inválido")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getCustomerByStatus(
            @Parameter(description = "Status do cliente", required = true,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"ACTIVE", "BLOCKED", "DISABLED"}))
            @PathVariable("status") String status) {
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
    @Operation(
            summary = "Clientes Ativos",
            description = "Retorna apenas clientes com status ACTIVE. " +
                    "Lista de clientes aptos para realizar compras."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes ativos retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {
        List<Customer> customers = service.getActiveCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/blocked")
    @Operation(
            summary = "Clientes Bloqueados",
            description = "Retorna clientes com status BLOCKED. " +
                    "Lista para revisão de bloqueios e possível reativação."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes bloqueados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getBlockedCustomer() {
        List<Customer> customers = service.getBlockedCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/disabled")
    @Operation(
            summary = "Clientes Desabilitados",
            description = "Retorna clientes com status DISABLED. " +
                    "Contas temporariamente desativadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes desabilitados retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<CustomerResponse>> getDisabledCustomer() {
        List<Customer> customers = service.getDisabledCustomers();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/profile")
    @Operation(
            summary = "Meu Perfil",
            description = "Retorna dados do perfil do usuário logado. " +
                    "Endpoint pessoal para visualização de dados próprios."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication authentication) {
        Customer customer = service.getMyProfile(authentication);
        return ResponseEntity.ok(CustomerMapper.toDTO(customer));
    }

    @PutMapping("/profile")
    @Operation(
            summary = "Atualizar Meu Perfil",
            description = "Permite ao usuário logado atualizar seus próprios dados. " +
                    "Endpoint seguro para autogestão de perfil."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
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

    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Ativar Cliente",
            description = "Ativa uma conta de cliente, permitindo compras e acesso completo. " +
                    "Operação administrativa para gestão de status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente ativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    public ResponseEntity<Object> activateCustomer(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Bloquear Cliente",
            description = "Bloqueia um cliente impedindo compras e acesso. " +
                    "Usado para clientes com problemas ou violações de política."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente bloqueado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    public ResponseEntity<Object> blockCustomer(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Desabilitar Cliente",
            description = "Desabilita temporariamente um cliente. " +
                    "Status intermediário entre ativo e bloqueado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente desabilitado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    public ResponseEntity<Object> disableCustomer(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Excluir Cliente",
            description = "Remove um cliente do sistema permanentemente. " +
                    "ATENÇÃO: Ação irreversível que pode afetar histórico de vendas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido ou cliente possui vendas associadas")
    })
    public ResponseEntity<Object> delete(
            @Parameter(description = "UUID do cliente", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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