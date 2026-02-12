package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.BookValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.lang.module.ResolutionException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository repository;
    private final BookValidator validator;
    private final AuthorService service;

    public BookService(BookRepository repository, BookValidator validator, AuthorService service) {
        this.repository = repository;
        this.validator = validator;
        this.service = service;
    }

    @Transactional
    public Book save(Book book, UUID authorId) {

        Author author = service.getForId(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + authorId));

        book.setAuthor(author);
        validator.valid(book);
        Book saved = repository.save(book);
        author.getBooks().add(saved);
        return saved;
    }

    @Transactional
    public Optional<Book> getForId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public List<Book> getAll(){
        return repository.findAll();
    }
}
