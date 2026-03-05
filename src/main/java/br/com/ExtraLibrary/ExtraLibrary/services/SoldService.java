package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.FormPayment;
import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.SoldValidator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SoldService {

    private static final Logger logger = LoggerFactory.getLogger(SoldService.class);

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
        return save(sold, null);
    }

    @Transactional
    public Sold save(Sold sold, Authentication authentication) {
        try {
            // 🔐 Validar autenticação se fornecida
            if (authentication != null) {
                validator.validateAuthentication(authentication);
                logger.debug("Authentication validated for user: {}", authentication.getName());
            }

            // ✅ Validar dados da venda
            validator.valid(sold);

            // 📦 Processar estoque dos livros
            processBookSales(sold.getBooksQuantity());

            // 💾 Salvar venda
            Sold savedSold = repository.save(sold);

            logger.info("Sale {} saved successfully for customer {} with total value {}",
                    savedSold.getId(),
                    savedSold.getCustomer().getId(),
                    savedSold.getFinalPrice());

            return savedSold;

        } catch (Exception e) {
            logger.error("Error saving sale for customer {}: {}",
                    sold.getCustomer() != null ? sold.getCustomer().getId() : "unknown",
                    e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Optional<Sold> getForId(Long id) {
        logger.debug("Retrieving sale with ID: {}", id);
        return repository.findById(id);
    }

    @Transactional
    public List<Sold> getAll() {
        logger.debug("Retrieving all sales");
        return repository.findAll();
    }

    @Transactional
    public List<Sold> getSalesByCustomer(UUID customerId) {
        logger.debug("Retrieving sales for customer: {}", customerId);
        return repository.findByCustomerId(customerId);
    }

    @Transactional
    public List<Sold> getSalesByCustomerAndPeriod(UUID customerId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Retrieving sales for customer {} between {} and {}", customerId, startDate, endDate);
        return repository.findByCustomerIdAndDateSaleBetween(customerId, startDate, endDate);
    }

    @Transactional
    public long countSalesByCustomer(UUID customerId) {
        logger.debug("Counting sales for customer: {}", customerId);
        return repository.countByCustomerId(customerId);
    }

    @Transactional
    public BigDecimal getTotalSalesByCustomer(UUID customerId) {
        logger.debug("Calculating total sales value for customer: {}", customerId);
        BigDecimal total = repository.sumTotalSalesByCustomer(customerId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public List<Sold> getSalesByPaymentMethod(FormPayment formPayment) {
        logger.debug("Retrieving sales by payment method: {}", formPayment);
        return repository.findByFormPayment(formPayment);
    }

    @Transactional
    public Sold update(Long id, Sold soldUpdate) {
        return update(id, soldUpdate, null);
    }

    @Transactional
    public Sold update(Long id, Sold soldUpdate, Authentication authentication) {
        try {
            // 🔐 Validar autenticação se fornecida
            if (authentication != null) {
                validator.validateAuthentication(authentication);
                logger.debug("Authentication validated for update by user: {}", authentication.getName());
            }

            // 🔍 Buscar venda existente
            Sold existingSold = getForId(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Venda com id: " + id + " não encontrada!"));

            // ✅ Validar se pode ser atualizada
            validator.validateUpdate(existingSold);

            // 🔄 Reverter estoque da venda original
            reverseBookSales(existingSold.getBooksQuantity());

            // 🆔 Manter ID
            soldUpdate.setId(id);

            // ✅ Validar nova venda
            validator.valid(soldUpdate);

            // 📦 Processar novo estoque
            processBookSales(soldUpdate.getBooksQuantity());

            // 💾 Salvar atualização
            Sold updatedSold = repository.save(soldUpdate);

            logger.info("Sale {} updated successfully by user: {}",
                    id,
                    authentication != null ? authentication.getName() : "system");

            return updatedSold;

        } catch (Exception e) {
            logger.error("Error updating sale {}: {}", id, e.getMessage());
            throw e;
        }
    }

    private void processBookSales(Map<UUID, Long> booksQuantity) {
        logger.debug("Processing book sales for {} different books", booksQuantity.size());

        for (Map.Entry<UUID, Long> entry : booksQuantity.entrySet()) {
            try {
                UUID bookId = entry.getKey();
                Long quantity = entry.getValue();

                logger.debug("Selling {} units of book {}", quantity, bookId);
                bookService.sellBookStock(bookId, quantity);

            } catch (Exception e) {
                logger.error("Error processing sale for book {}: {}", entry.getKey(), e.getMessage());
                throw new IllegalArgumentException("Erro ao processar venda do livro " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void reverseBookSales(Map<UUID, Long> booksQuantity) {
        logger.debug("Reversing book sales for {} different books", booksQuantity.size());

        for (Map.Entry<UUID, Long> entry : booksQuantity.entrySet()) {
            try {
                UUID bookId = entry.getKey();
                Long quantity = entry.getValue();

                logger.debug("Reversing {} units of book {}", quantity, bookId);
                bookService.addBookStock(bookId, quantity);

            } catch (Exception e) {
                logger.warn("Error reversing stock for book {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }
}
