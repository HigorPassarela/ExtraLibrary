package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookGender;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.BookValidator;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository repository;
    private final BookValidator validator;
    private final AuthorService authorService;

    public BookService(BookRepository repository, BookValidator validator, AuthorService authorService) {
        this.repository = repository;
        this.validator = validator;
        this.authorService = authorService;
    }

    private void updateBookStatus(Book book) {
        if (book.getQuantity() <= 0) {
            book.setBookStatus(BookStatus.OUT_OF_STOCK);
        } else {
            book.setBookStatus(BookStatus.IN_STOCK);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Book save(Book book, UUID authorId) {
        Author author = authorService.getForId(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Autor não encontrado com ID: " + authorId));

        book.setAuthor(author);
        updateBookStatus(book);
        validator.valid(book);
        Book saved = repository.save(book);
        author.getBooks().add(saved);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public Optional<Book> getForId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public List<Book> getAll() {
        return repository.findAll();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Book update(UUID id, Book bookUpdate, UUID idAuthor) {
        Book existingBook = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (!existingBook.getAuthor().getId().equals(idAuthor)) {
            Author newAuthor = authorService.getForId(idAuthor)
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
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Book sellBookStock(UUID id, Long quantityToSell) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado"));

        if (quantityToSell <= 0) {
            throw new IllegalArgumentException("Quantidade para venda deve ser maior que zero!");
        }

        if (book.getQuantity() < quantityToSell) {
            throw new IllegalArgumentException("Quantidade insuficiente em estoque. Disponível: " + book.getQuantity());
        }

        Long newQuantity = book.getQuantity() - quantityToSell;
        book.setQuantity(newQuantity);
        updateBookStatus(book);

        return repository.save(book);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Book addBookStock(UUID id, Long quantityToAdd) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Quantidade para adicionar deve ser maior do que zero!");
        }

        Long newQuantity = book.getQuantity() + quantityToAdd;
        book.setQuantity(newQuantity);
        updateBookStatus(book);

        return repository.save(book);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        if (book.getAuthor() != null) {
            book.getAuthor().getBooks().remove(book);
        }

        repository.delete(book);
    }

    // Novos métodos de busca
    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public List<Book> findByTitle(String title) {
        return repository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public Optional<Book> findByIsbn(String isbn) {
        return repository.findByIsbn(isbn);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public List<Book> findByAuthor(UUID authorId) {
        return repository.findByAuthorId(authorId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public List<Book> findByGender(String gender) {
        BookGender bookGender = BookGender.valueOf(gender.toUpperCase());
        return repository.findByGender(bookGender);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public List<Book> findAvailableBooks() {
        return repository.findByBookStatus(BookStatus.IN_STOCK);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Object getInventoryReport() {
        long totalBooks = repository.count();
        long inStock = repository.countByBookStatus(BookStatus.IN_STOCK);
        long outOfStock = repository.countByBookStatus(BookStatus.OUT_OF_STOCK);
        BigDecimal totalValue = repository.sumTotalInventoryValue();
        long lowStockBooks = repository.countByQuantityLessThan(5L);

        return Map.of(
                "totalBooks", totalBooks,
                "inStock", inStock,
                "outOfStock", outOfStock,
                "totalValue", totalValue != null ? totalValue : BigDecimal.ZERO,
                "lowStockBooks", lowStockBooks,
                "stockPercentage", totalBooks > 0 ? (inStock * 100.0 / totalBooks) : 0
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public List<Book> findLowStockBooks(Long threshold) {
        return repository.findByQuantityLessThan(threshold);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public Book updateStatus(UUID id, BookStatus status) {
        Book book = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro com Id: " + id + " não encontrado!"));

        book.setBookStatus(status);
        return repository.save(book);
    }
}