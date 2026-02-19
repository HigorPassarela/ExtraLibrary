package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.SoldValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SoldService {

    private final SoldRepository repository;
    private final SoldValidator validator;
    private final BookService bookService;

    public SoldService(SoldRepository repository, SoldValidator validator, BookService bookService) {
        this.repository = repository;
        this.validator = validator;
        this.bookService = bookService;
    }

    @Transactional
    public Sold save(Sold sold) {
        validator.valid(sold);

        processBookSales(sold.getBooksQuantity());

        return repository.save(sold);
    }

    @Transactional
    public Optional<Sold> getForId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public List<Sold> getAll() {
        return repository.findAll();
    }

    @Transactional
    public List<Sold> getSalesByCustomer(UUID customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Transactional
    public List<Sold> getSalesByCustomerAndPeriod(UUID customerId, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findByCustomerIdAndDateSaleBetween(customerId, startDate, endDate);
    }

    @Transactional
    public long countSalesByCustomer(UUID customerId) {
        return repository.countByCustomerId(customerId);
    }

    @Transactional
    public BigDecimal getTotalSalesByCustomer(UUID customerId) {
        BigDecimal total = repository.sumTotalSalesByCustomer(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public List<Sold> getSalesByPaymentMethod(FormPayment formPayment) {
        return repository.findByFormPayment(formPayment);
    }

//    @Transactional
//    public List<Sold> getCashSales() {
//        return getSalesByPaymentMethod(FormPayment.CASH);
//    }
//
//    @Transactional
//    public List<Sold> getDebitSales() {
//        return getSalesByPaymentMethod(FormPayment.DEBIT_CARD);
//    }
//
//    @Transactional
//    public List<Sold> getCreditSales() {
//        return getSalesByPaymentMethod(FormPayment.CREDIT_CARD);
//    }
//
//    @Transactional
//    public List<Sold> getPixSales() {
//        return getSalesByPaymentMethod(FormPayment.PIX);
//    }
//
//    @Transactional
//    public List<Sold> getBankTransferSales() {
//        return getSalesByPaymentMethod(FormPayment.BANK_TRANSFER);
//    }

    @Transactional
    public Sold update(Long id, Sold soldUpdate) {
        Sold existingSold = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venda com id: " + id + " não encontrada!"));

        validator.validateUpdate(existingSold);

        reverseBookSales(existingSold.getBooksQuantity());

        soldUpdate.setId(id);

        validator.valid(soldUpdate);

        processBookSales(soldUpdate.getBooksQuantity());

        return repository.save(soldUpdate);
    }

    private void processBookSales(Map<UUID, Long> booksQuantity) {
        for (Map.Entry<UUID, Long> entry : booksQuantity.entrySet()) {
            try {
                bookService.sellBookStock(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                throw new IllegalArgumentException("Erro ao processar venda do livro " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void reverseBookSales(Map<UUID, Long> booksQuantity) {
        for (Map.Entry<UUID, Long> entry : booksQuantity.entrySet()) {
            try {
                bookService.addBookStock(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                System.err.println("Erro ao reverter estoque do livro " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }
}
