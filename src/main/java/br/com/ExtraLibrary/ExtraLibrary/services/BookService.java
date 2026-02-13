package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.BookValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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

    private void updateBookStatus(Book book) {
        if (book.getQuantity() <= 0) {
            book.setBookStatus(BookStatus.OUT_OF_STOCK);
        } else {
            book.setBookStatus(BookStatus.IN_STOCK);
        }
    }

    @Transactional
    public Book save(Book book, UUID authorId) {

        Author author = service.getForId(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + authorId));

        book.setAuthor(author);
        updateBookStatus(book);
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
    public List<Book> getAll() {
        return repository.findAll();
    }

    @Transactional
    public Book update(UUID id, Book bookUpdate, UUID idAuthor) {
        Book existingBook = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (!existingBook.getAuthor().getId().equals(idAuthor)) {
            Author newAuthor = service.getForId(idAuthor)
                    .orElseThrow(() -> new ResourceNotFoundException("Autor com Id: " + idAuthor + " não encontrado"));

            existingBook.getAuthor().getBooks().remove(existingBook);

            bookUpdate.setAuthor(newAuthor);
            newAuthor.getBooks().add(bookUpdate);
        } else {
            bookUpdate.setAuthor(existingBook.getAuthor());
        }

        bookUpdate.setId(id);
        updateBookStatus(bookUpdate);
        validator.valid(bookUpdate);

        return repository.save(bookUpdate);
    }

    @Transactional
    public Book updatedQuantity(UUID id, Long newQuantity) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (newQuantity < 0 ){
            throw new IllegalArgumentException("Quantidade não pode ser negativa!");
        }

        book.setQuantity(newQuantity);
        updateBookStatus(book);

        return repository.save(book);
    }

    @Transactional
    public Book sellBookStock(UUID id, Long quantityToSell) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + "não encontrado"));

        if (quantityToSell <= 0) {
            throw new IllegalArgumentException("Quantidade para venda deve ser maior que zero!");
        }

        if (book.getQuantity() < quantityToSell) {
            throw new IllegalArgumentException("Quantidade insuficiente em estoque. Diponivel: " + book.getQuantity());
        }

        Long newQuantity = book.getQuantity() - quantityToSell;
        book.setQuantity(newQuantity);
        updateBookStatus(book);

        return repository.save(book);
    }

    @Transactional
    public Book addBookStock(UUID id, Long quantityToAdd) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantidade para adicionar este livro deve ser maior do que zero!");
        }

        Long newQuantity = book.getQuantity() + quantityToAdd;
        book.setQuantity(newQuantity);
        updateBookStatus(book);

        return repository.save(book);
    }

    @Transactional
    public void delete(UUID id) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        book.getAuthor().getBooks().remove(book);

        repository.delete(book);
    }
}
