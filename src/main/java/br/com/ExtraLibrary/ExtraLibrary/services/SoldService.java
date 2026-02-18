package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.SoldValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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

        processBookSales(sold.getBookIds());

        return repository.save(sold);
    }

    private void processBookSales(List<UUID> bookIds) {
        for (UUID bookId : bookIds) {
            try {
                bookService.sellBookStock(bookId, 1L); // Vender 1 unidade de cada livro
            } catch (Exception e) {
                throw new IllegalArgumentException("Erro ao processar venda do livro " + bookId + ": " + e.getMessage());
            }
        }
    }
}
