package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.SoldValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
