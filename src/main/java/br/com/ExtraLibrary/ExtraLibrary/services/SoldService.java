package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.repository.SoldRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.SoldValidator;
import org.springframework.stereotype.Service;

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
}
