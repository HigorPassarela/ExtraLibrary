package br.com.ExtraLibrary.ExtraLibrary.validators;

import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.CustomerStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import br.com.ExtraLibrary.ExtraLibrary.repository.CustomerRepository;
import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SoldValidator {

    private static final Logger logger = LoggerFactory.getLogger(SoldValidator.class);

    private final SoldRepository soldRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;

    public SoldValidator(SoldRepository soldRepository, CustomerRepository customerRepository, BookRepository bookRepository) {
        this.soldRepository = soldRepository;
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
    }

    public void valid(Sold sold) {
        logger.debug("Starting validation for sale");

        validateBasicFields(sold);
        validateCustomer(sold);
        validateBooks(sold);
        calculateAndValidatePrices(sold);
        validatePaymentForm(sold);
        validateDuplicateSale(sold);

        logger.debug("Sale validation completed successfully");
    }

    /**
     * 🔐 Validar se o usuário está autenticado
     */
    public void validateAuthentication(Authentication authentication) {
        if (authentication == null) {
            logger.warn("Authentication is null");
            throw new IllegalArgumentException("Usuário deve estar autenticado para realizar compras");
        }

        if (!authentication.isAuthenticated()) {
            logger.warn("User is not authenticated: {}", authentication.getName());
            throw new IllegalArgumentException("Usuário deve estar autenticado para realizar compras");
        }

        String username = authentication.getName();
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Username is empty or null");
            throw new IllegalArgumentException("Usuário inválido");
        }

        logger.debug("Authentication validated for user: {}", username);
    }

    private void validateBasicFields(Sold sold) {
        logger.debug("Validating basic fields");

        if (sold.getDateSale() == null) {
            sold.setDateSale(LocalDateTime.now());
        }

        if (sold.getFormPayment() == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória");
        }

        if (sold.getBooksQuantity() == null || sold.getBooksQuantity().isEmpty()) {
            throw new IllegalArgumentException("Venda deve conter pelo menos um livro");
        }

        // Verificar se há IDs de livros duplicados
        long distinctCount = sold.getBooksQuantity().keySet().stream().distinct().count();
        if (distinctCount != sold.getBooksQuantity().size()) {
            throw new IllegalArgumentException("Lista de livros contém IDs duplicados");
        }

        logger.debug("Basic fields validation passed");
    }

    private void validateCustomer(Sold sold) {
        logger.debug("Validating customer");

        if (sold.getCustomer() == null || sold.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Cliente é obrigatório");
        }

        UUID customerId = sold.getCustomer().getId();
        Optional<Customer> customerOpt = customerRepository.findById(customerId);

        if (customerOpt.isEmpty()) {
            logger.warn("Customer not found: {}", customerId);
            throw new ResourceNotFoundException("Cliente com Id: " + customerId + " não encontrado!");
        }

        Customer customer = customerOpt.get();

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            logger.warn("Customer {} has invalid status: {}", customerId, customer.getStatus());
            throw new IllegalArgumentException(
                    "Cliente não pode fazer compras. Status atual: " + customer.getStatus() +
                            ". Apenas clientes com status ACTIVE podem fazer compras."
            );
        }

        sold.setCustomer(customer);
        logger.debug("Customer validation passed for: {}", customerId);
    }

    private void validateBooks(Sold sold) {
        logger.debug("Validating books and quantities");

        if (sold.getBooksQuantity() == null || sold.getBooksQuantity().isEmpty()) {
            throw new IllegalArgumentException("Venda deve conter pelo menos um livro");
        }

        for (Map.Entry<UUID, Long> entry : sold.getBooksQuantity().entrySet()) {
            UUID bookId = entry.getKey();
            Long quantity = entry.getValue();

            if (bookId == null) {
                throw new IllegalArgumentException("ID do livro não pode ser nulo");
            }

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantidade deve ser maior que zero para o livro: " + bookId);
            }

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                logger.warn("Book not found: {}", bookId);
                throw new ResourceNotFoundException("Livro com Id: " + bookId + " não encontrado!");
            }

            Book book = bookOpt.get();

            if (book.getQuantity() == null || book.getQuantity() < quantity) {
                logger.warn("Insufficient stock for book {}: available={}, requested={}",
                        bookId, book.getQuantity(), quantity);
                throw new IllegalArgumentException(
                        "Estoque insuficiente para '" + book.getTitle() + "'. " +
                                "Disponível: " + (book.getQuantity() != null ? book.getQuantity() : 0) +
                                ", Solicitado: " + quantity
                );
            }
        }

        logger.debug("Books validation passed for {} items", sold.getBooksQuantity().size());
    }

    private void calculateAndValidatePrices(Sold sold) {
        logger.debug("Calculating and validating prices");

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        // Calcular subtotal baseado nos livros e quantidades
        for (Map.Entry<UUID, Long> entry : sold.getBooksQuantity().entrySet()) {
            UUID bookId = entry.getKey();
            Long quantity = entry.getValue();

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                BigDecimal itemTotal = book.getPrice().multiply(BigDecimal.valueOf(quantity));
                calculatedSubtotal = calculatedSubtotal.add(itemTotal);
            }
        }

        sold.setSubtotal(calculatedSubtotal);

        // Validar e ajustar desconto
        BigDecimal discount = sold.getDiscount();
        if (discount == null) {
            discount = BigDecimal.ZERO;
            sold.setDiscount(discount);
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Desconto não pode ser negativo");
        }

        if (discount.compareTo(calculatedSubtotal) > 0) {
            throw new IllegalArgumentException(
                    "Desconto (R$ " + discount + ") não pode ser maior que o subtotal (R$ " + calculatedSubtotal + ")"
            );
        }

        // Calcular preço final
        BigDecimal finalPrice = calculatedSubtotal.subtract(discount);

        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço final deve ser maior que zero");
        }

        sold.setFinalPrice(finalPrice);

        logger.debug("Price calculation completed - Subtotal: {}, Discount: {}, Final: {}",
                calculatedSubtotal, discount, finalPrice);
    }

    private void validatePaymentForm(Sold sold) {
        logger.debug("Validating payment form: {}", sold.getFormPayment());

        switch (sold.getFormPayment()) {
            case CASH:
                logger.debug("Cash payment - no restrictions");
                break;
            case CREDIT_CARD:
            case DEBIT_CARD:
                if (sold.getFinalPrice().compareTo(new BigDecimal("5.00")) < 0) {
                    throw new IllegalArgumentException("Valor mínimo para pagamento com cartão é R$ 5,00");
                }
                logger.debug("Card payment validated - amount: {}", sold.getFinalPrice());
                break;
            case PIX:
                logger.debug("PIX payment - no restrictions");
                break;
            case BANK_TRANSFER:
                if (sold.getFinalPrice().compareTo(new BigDecimal("10.00")) < 0) {
                    throw new IllegalArgumentException("Valor mínimo para transferência bancária é R$ 10,00");
                }
                logger.debug("Bank transfer validated - amount: {}", sold.getFinalPrice());
                break;
            default:
                throw new IllegalArgumentException("Forma de pagamento não suportada: " + sold.getFormPayment());
        }
    }

    private void validateDuplicateSale(Sold sold) {
        logger.debug("Validating duplicate sales");

        if (sold.getCustomer() != null && sold.getDateSale() != null) {
            LocalDateTime startTime = sold.getDateSale().minusMinutes(1);
            LocalDateTime endTime = sold.getDateSale().plusMinutes(1);

            var recentSales = soldRepository.findByCustomerIdAndDateSaleBetween(
                    sold.getCustomer().getId(),
                    startTime,
                    endTime
            );

            for (Sold recentSale : recentSales) {
                // Pular se for a mesma venda (no caso de update)
                if (sold.getId() != null && sold.getId().equals(recentSale.getId())) {
                    continue;
                }

                // Verificar se é uma venda similar
                if (recentSale.getBooksQuantity().size() == sold.getBooksQuantity().size() &&
                        recentSale.getBooksQuantity().keySet().containsAll(sold.getBooksQuantity().keySet()) &&
                        recentSale.getFinalPrice().compareTo(sold.getFinalPrice()) == 0) {

                    logger.warn("Duplicate sale detected for customer {}", sold.getCustomer().getId());
                    throw new DuplicatedRegisterException(
                            "Possível venda duplicada detectada. Já existe uma venda similar para este cliente nos últimos minutos."
                    );
                }
            }
        }

        logger.debug("Duplicate sale validation passed");
    }

    public void validateDeletion(Sold sold) {
        logger.debug("Validating deletion permissions for sale: {}", sold.getId());

        if (sold.getDateSale() != null) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

            if (sold.getDateSale().isBefore(cutoffTime)) {
                logger.warn("Attempt to delete old sale: {} (created: {})", sold.getId(), sold.getDateSale());
                throw new IllegalArgumentException(
                        "Não é possível cancelar vendas realizadas há mais de 24 horas"
                );
            }
        }

        logger.debug("Deletion validation passed for sale: {}", sold.getId());
    }

    public void validateUpdate(Sold sold) {
        logger.debug("Validating update permissions for sale: {}", sold.getId());

        if (sold.getDateSale() != null) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);

            if (sold.getDateSale().isBefore(cutoffTime)) {
                logger.warn("Attempt to update old sale: {} (created: {})", sold.getId(), sold.getDateSale());
                throw new IllegalArgumentException(
                        "Não é possível alterar vendas realizadas há mais de 2 horas"
                );
            }
        }

        logger.debug("Update validation passed for sale: {}", sold.getId());
    }
}
