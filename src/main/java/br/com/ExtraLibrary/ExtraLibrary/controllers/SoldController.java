package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.SoldRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.SoldResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.SoldMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import br.com.ExtraLibrary.ExtraLibrary.services.SoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sold")
@Tag(name = "Sales Management", description = "Gerenciamento de Vendas - Sistema Completo de Transações e Relatórios")
@SecurityRequirement(name = "Bearer Authentication")
public class SoldController {

    private static final Logger logger = LoggerFactory.getLogger(SoldController.class);

    private final SoldService service;
    private final SoldMapper mapper;

    public SoldController(SoldService service, SoldMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(
            summary = "Registrar Nova Venda",
            description = "Processa uma nova venda de livros para clientes. " +
                    "Funcionalidades: Valida estoque, calcula preços, atualiza inventário automaticamente. " +
                    "REQUER AUTENTICAÇÃO JWT - Todas as vendas são auditadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venda registrada com sucesso - Estoque atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou estoque insuficiente"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente - Login obrigatório"),
            @ApiResponse(responseCode = "404", description = "Cliente ou livro não encontrado"),
            @ApiResponse(responseCode = "409", description = "Venda duplicada detectada")
    })
    public ResponseEntity<Object> save(
            @RequestBody @Valid SoldRequest soldRequest,
            Authentication authentication) {
        try {
            // Verificação adicional de autenticação (dupla segurança)
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Attempt to make purchase without authentication");
                var error = ErrorResponse.unauthorized("Usuário deve estar logado para realizar compras");
                return ResponseEntity.status(error.status()).body(error);
            }

            // Log de auditoria - quem está fazendo a compra
            String userEmail = authentication.getName();
            String authorities = authentication.getAuthorities().toString();

            logger.info("User {} (roles: {}) is making a purchase for customer {}",
                    userEmail, authorities, soldRequest.customerId());

            // Processar a venda
            Sold sold = SoldMapper.toEntity(soldRequest);
            Sold savedSold = service.save(sold, authentication);

            // Criar URI de localização do recurso criado
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedSold.getId())
                    .toUri();

            logger.info("Sale {} successfully created by user {} for customer {} - Total: R$ {}",
                    savedSold.getId(), userEmail, soldRequest.customerId(), savedSold.getFinalPrice());

            // Resposta de sucesso com detalhes da venda
            SoldResponse soldResponse = mapper.toDTO(savedSold);
            return ResponseEntity.created(location).body(Map.of(
                    "success", true,
                    "message", "Venda registrada com sucesso!",
                    "sale", soldResponse,
                    "createdBy", userEmail,
                    "createdAt", savedSold.getCreatedAt()
            ));

        } catch (DuplicatedRegisterException e) {
            logger.warn("Duplicate sale attempt by {}: {}",
                    authentication != null ? authentication.getName() : "unknown", e.getMessage());
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid sale data from {}: {}",
                    authentication != null ? authentication.getName() : "unknown", e.getMessage());
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found during sale by {}: {}",
                    authentication != null ? authentication.getName() : "unknown", e.getMessage());
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error during sale creation by {}: {}",
                    authentication != null ? authentication.getName() : "unknown", e.getMessage(), e);
            var error = ErrorResponse.internalServerError("Erro interno do servidor ao processar venda");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar Venda por ID",
            description = "Retorna detalhes completos de uma venda específica: cliente, livros, valores, forma de pagamento. " +
                    "Útil para: Confirmação de compras, suporte ao cliente, auditoria de vendas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes da venda retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    public ResponseEntity<Object> getDetailsFromId(
            @Parameter(description = "ID da venda", required = true, example = "1")
            @PathVariable String id,
            Authentication authentication) {
        try {
            // Verificação de autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                var error = ErrorResponse.unauthorized("Acesso negado. Faça login para consultar vendas.");
                return ResponseEntity.status(error.status()).body(error);
            }

            Long soldId = Long.parseLong(id);
            String userEmail = authentication.getName();

            logger.debug("User {} requesting sale details for ID: {}", userEmail, soldId);

            return service
                    .getForId(soldId)
                    .map(sold -> {
                        SoldResponse soldResponse = mapper.toDTO(sold);
                        return ResponseEntity.ok((Object) soldResponse);
                    })
                    .orElseGet(() -> {
                        var error = ErrorResponse.notFound("Venda com ID " + soldId + " não encontrada");
                        return ResponseEntity.status(error.status()).body(error);
                    });

        } catch (NumberFormatException e) {
            var error = ErrorResponse.badrequest("ID da venda deve ser um número válido");
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving sale {}: {}", id, e.getMessage());
            var error = ErrorResponse.internalServerError("Erro ao consultar venda");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping
    @Operation(
            summary = "Listar Todas as Vendas",
            description = "Retorna histórico completo de vendas do sistema. " +
                    "Informações incluídas: Data, cliente, produtos, valores, forma de pagamento. " +
                    "Recomendado para: Relatórios gerenciais, análise de vendas, auditoria."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vendas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente")
    })
    public ResponseEntity<Object> getAll(Authentication authentication) {
        try {
            // Verificação de autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                var error = ErrorResponse.unauthorized("Acesso negado. Faça login para consultar vendas.");
                return ResponseEntity.status(error.status()).body(error);
            }

            String userEmail = authentication.getName();
            logger.info("User {} requesting all sales", userEmail);

            List<Sold> solds = service.getAll();
            List<SoldResponse> soldResponses = solds.stream()
                    .map(mapper::toDTO)
                    .toList();

            logger.debug("Returning {} sales to user {}", soldResponses.size(), userEmail);
            return ResponseEntity.ok((Object) soldResponses);

        } catch (Exception e) {
            logger.error("Error retrieving all sales: {}", e.getMessage());
            var error = ErrorResponse.internalServerError("Erro ao consultar vendas");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Histórico de Compras do Cliente",
            description = "Retorna todas as compras realizadas por um cliente específico. " +
                    "Útil para: Atendimento ao cliente, análise de comportamento de compra, fidelização."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico de compras retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "ID do cliente inválido")
    })
    public ResponseEntity<Object> getSalesByCustomer(
            @Parameter(description = "UUID do cliente", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("customerId") String customerId,
            Authentication authentication) {
        try {
            // Verificação de autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                var error = ErrorResponse.unauthorized("Acesso negado. Faça login para consultar vendas.");
                return ResponseEntity.status(error.status()).body(error);
            }

            UUID customerUUID = UUID.fromString(customerId);
            String userEmail = authentication.getName();

            logger.info("User {} requesting sales for customer {}", userEmail, customerUUID);

            List<Sold> solds = service.getSalesByCustomer(customerUUID);
            List<SoldResponse> soldResponses = solds.stream()
                    .map(mapper::toDTO)
                    .toList();

            return ResponseEntity.ok((Object) soldResponses);

        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest("ID do cliente inválido: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving sales for customer {}: {}", customerId, e.getMessage());
            var error = ErrorResponse.internalServerError("Erro ao consultar vendas do cliente");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/payment/{paymentMethod}/stats")
    @Operation(
            summary = "Relatório por Forma de Pagamento",
            description = "Analisa vendas segmentadas por método de pagamento. " +
                    "Métodos disponíveis: CASH, CREDIT_CARD, DEBIT_CARD, PIX, BANK_TRANSFER. " +
                    "Útil para: Análise financeira, preferências de pagamento, planejamento."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório de pagamento gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "Forma de pagamento inválida")
    })
    public ResponseEntity<Object> getPaymentMethodStats(
            @Parameter(description = "Forma de pagamento", required = true,
                    example = "CASH",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"CASH", "CREDIT_CARD", "DEBIT_CARD", "PIX", "BANK_TRANSFER"}
                    ))
            @PathVariable("paymentMethod") String paymentMethod,
            Authentication authentication) {
        try {
            // Verificação de autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                var error = ErrorResponse.unauthorized("Acesso negado. Faça login para consultar estatísticas.");
                return ResponseEntity.status(error.status()).body(error);
            }

            FormPayment formPayment = FormPayment.valueOf(paymentMethod.toUpperCase());
            String userEmail = authentication.getName();

            logger.info("User {} requesting payment method stats for: {}", userEmail, formPayment);

            List<Sold> sales = service.getSalesByPaymentMethod(formPayment);
            List<SoldResponse> responses = sales.stream()
                    .map(mapper::toDTO)
                    .toList();

            return ResponseEntity.ok((Object) responses);

        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(
                    "Forma de pagamento inválida. Use: CASH, CREDIT_CARD, DEBIT_CARD, PIX, BANK_TRANSFER"
            );
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving payment method stats: {}", e.getMessage());
            var error = ErrorResponse.internalServerError("Erro ao consultar estatísticas de pagamento");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/customer/{customerId}/stats")
    @Operation(
            summary = "Estatísticas Detalhadas do Cliente",
            description = "Gera relatório completo de um cliente: total de compras, valor gasto, frequência. " +
                    "Informações retornadas: Quantidade de vendas, valor total investido, ticket médio. " +
                    "Útil para: Programa de fidelidade, análise de valor do cliente (CLV)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas do cliente calculadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "ID do cliente inválido")
    })
    public ResponseEntity<Object> getCustomerStats(
            @Parameter(description = "UUID do cliente", required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("customerId") String customerId,
            Authentication authentication) {
        try {
            // Verificação de autenticação
            if (authentication == null || !authentication.isAuthenticated()) {
                var error = ErrorResponse.unauthorized("Acesso negado. Faça login para consultar estatísticas.");
                return ResponseEntity.status(error.status()).body(error);
            }

            UUID customerUUID = UUID.fromString(customerId);
            String userEmail = authentication.getName();

            logger.info("User {} requesting customer stats for: {}", userEmail, customerUUID);

            long salesCount = service.countSalesByCustomer(customerUUID);
            BigDecimal totalValue = service.getTotalSalesByCustomer(customerUUID);

            var stats = new CustomerSalesStats(customerUUID, salesCount, totalValue);
            return ResponseEntity.ok((Object) stats);

        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest("ID do cliente inválido: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Error retrieving customer stats for {}: {}", customerId, e.getMessage());
            var error = ErrorResponse.internalServerError("Erro ao consultar estatísticas do cliente");
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // Classe interna para estatísticas do cliente
    public static class CustomerSalesStats {
        private final UUID customerId;
        private final long salesCount;
        private final BigDecimal totalValue;

        public CustomerSalesStats(UUID customerId, long salesCount, BigDecimal totalValue) {
            this.customerId = customerId;
            this.salesCount = salesCount;
            this.totalValue = totalValue != null ? totalValue : BigDecimal.ZERO;
        }

        public UUID getCustomerId() {
            return customerId;
        }

        public long getSalesCount() {
            return salesCount;
        }

        public BigDecimal getTotalValue() {
            return totalValue;
        }
    }
}