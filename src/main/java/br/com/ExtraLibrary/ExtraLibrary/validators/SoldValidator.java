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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class SoldValidator {

    private SoldRepository soldRepository;
    private CustomerRepository customerRepository;
    private BookRepository bookRepository;

    public SoldValidator(SoldRepository soldRepository, CustomerRepository customerRepository, BookRepository bookRepository) {
        this.soldRepository = soldRepository;
        this.customerRepository = customerRepository;
        this.bookRepository = bookRepository;
    }

    public void valid(Sold sold){
        validateBasicFields(sold);
        validateCustomer(sold);
        validateBooks(sold);
        validatePrices(sold);
        validatePaymentForm(sold);
        validateDuplicateSale(sold);
        validateDeletion(sold);
        validateUpdate(sold);
    }

    private void validateBasicFields(Sold sold) {
        if (sold.getDateSale() == null) {
            sold.setDateSale(LocalDateTime.now());
        }

        if (sold.getFormPayment() == null) {
            throw new IllegalArgumentException("Forma de pagamento é obrigatória");
        }

        if (sold.getBookIds() == null || sold.getBookIds().isEmpty()) {
            throw new IllegalArgumentException("Venda deve conter pelo menos um livro");
        }

        long distinctCount = sold.getBookIds().stream().distinct().count();
        if (distinctCount != sold.getBookIds().size()) {
            throw new IllegalArgumentException("Lista de livros contém IDs duplicados");
        }
    }

    private void validateCustomer(Sold sold) {
        if (sold.getCustomer() == null || sold.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Cliente é obrigatório");
        }

        UUID customerId = sold.getCustomer().getId();
        Optional<Customer> customerOpt = customerRepository.findById(customerId);

        if (customerOpt.isEmpty()) {
            throw new ResourceNotFoundException("Cliente com Id: " + customerId + " não encontrado!");
        }

        Customer customer = customerOpt.get();

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cliente não pode fazer compras. Status atual: " + customer.getStatus() +
                            ". Apenas clientes com status ACTIVE podem fazer compras."
            );
        }

        sold.setCustomer(customer);
    }

    private void validateBooks(Sold sold) {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (UUID bookId : sold.getBookIds()) {
            if (bookId == null) {
                throw new IllegalArgumentException("ID do livro não pode ser nulo");
            }

            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                throw new ResourceNotFoundException("Livro com Id: " + bookId + " não encontrado!");
            }

            Book book = bookOpt.get();

            if (book.getQuantity() == null || book.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Livro '" + book.getTitle() + "' está fora de estoque. Quantidade disponível: " +
                                (book.getQuantity() != null ? book.getQuantity() : 0)
                );
            }

            if (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Livro '" + book.getTitle() + "' não possui preço válido");
            }

            calculatedSubtotal = calculatedSubtotal.add(book.getPrice());
        }

        sold.setSubtotal(calculatedSubtotal);
    }

    private void validatePrices(Sold sold) {
        if (sold.getSubtotal() == null || sold.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Subtotal deve ser maior que zero");
        }

        if (sold.getDiscount() != null) {
            if (sold.getDiscount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Desconto não pode ser negativo");
            }

            if (sold.getDiscount().compareTo(sold.getSubtotal()) > 0) {
                throw new IllegalArgumentException(
                        "Desconto (R$ " + sold.getDiscount() + ") não pode ser maior que o subtotal (R$ " + sold.getSubtotal() + ")"
                );
            }
        }

        BigDecimal discount = sold.getDiscount() != null ? sold.getDiscount() : BigDecimal.ZERO;
        BigDecimal finalPrice = sold.getSubtotal().subtract(discount);

        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Preço final deve ser maior que zero");
        }

        sold.setFinalPrice(finalPrice);
    }

    private void validatePaymentForm(Sold sold) {
        switch (sold.getFormPayment()) {
            case CASH:
                break;
            case CREDIT_CARD:
            case DEBIT_CARD:
                if (sold.getFinalPrice().compareTo(new BigDecimal("5.00")) < 0) {
                    throw new IllegalArgumentException("Valor mínimo para pagamento com cartão é R$ 5,00");
                }
                break;
            case PIX:
                break;
            case BANK_TRANSFER:
                if (sold.getFinalPrice().compareTo(new BigDecimal("10.00")) < 0) {
                    throw new IllegalArgumentException("Valor mínimo para transferência bancária é R$ 10,00");
                }
                break;
            default:
                throw new IllegalArgumentException("Forma de pagamento não suportada: " + sold.getFormPayment());
        }
    }

    private void validateDuplicateSale(Sold sold) {
        if (sold.getCustomer() != null && sold.getDateSale() != null) {
            LocalDateTime startTime = sold.getDateSale().minusMinutes(1);
            LocalDateTime endTime = sold.getDateSale().plusMinutes(1);

            var recentSales = soldRepository.findByCustomerIdAndDateSaleBetween(
                    sold.getCustomer().getId(),
                    startTime,
                    endTime
            );

            for (Sold recentSale : recentSales) {
                if (sold.getId() != null && sold.getId().equals(recentSale.getId())) {
                    continue;
                }

                if (recentSale.getBookIds().size() == sold.getBookIds().size() &&
                        recentSale.getBookIds().containsAll(sold.getBookIds()) &&
                        recentSale.getFinalPrice().compareTo(sold.getFinalPrice()) == 0) {

                    throw new DuplicatedRegisterException(
                            "Possível venda duplicada detectada. Já existe uma venda similar para este cliente nos últimos minutos."
                    );
                }
            }
        }
    }

    public void validateDeletion(Sold sold) {
        if (sold.getDateSale() != null) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

            if (sold.getDateSale().isBefore(cutoffTime)) {
                throw new IllegalArgumentException(
                        "Não é possível cancelar vendas realizadas há mais de 24 horas"
                );
            }
        }
    }

    public void validateUpdate(Sold sold) {
        if (sold.getDateSale() != null) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);

            if (sold.getDateSale().isBefore(cutoffTime)) {
                throw new IllegalArgumentException(
                        "Não é possível alterar vendas realizadas há mais de 2 horas"
                );
            }
        }
    }
}
