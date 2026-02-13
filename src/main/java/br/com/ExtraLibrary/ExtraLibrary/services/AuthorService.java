package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.repository.AuthorRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.AuthorValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthorService {

    private final AuthorRepository repository;
    private final AuthorValidator validator;

    public AuthorService(AuthorRepository repository, AuthorValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public Author save(Author author) {
        validator.valid(author);
        return repository.save(author);
    }

    @Transactional
    public Optional<Author> getForId(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public List<Author> getAll() {
        return repository.findAll();
    }

    @Transactional
    public List<Author> findByName(String name) {
        return repository.findByName(name);
    }

    @Transactional
    public List<Author> findByNacionality(String nacionality) {
        return repository.findByNacionality(nacionality);
    }

    @Transactional
    public AuthorStats getAuthorStats(UUID id) {
        Author author = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor com id: " + id + " não encontrado!"));

        int totalBooks = author.getBooks().size();
        long totalQuantity = author.getBooks().stream()
                .mapToLong(Book::getQuantity)
                .sum();

        return new AuthorStats(author.getName(), totalBooks, totalQuantity);
    }

    @Transactional
    public Author update(UUID id, Author authorUpdate) {
        Author existingAuthor = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor com id: " + id + " não encontrado!"));

        authorUpdate.setBooks(existingAuthor.getBooks());
        authorUpdate.setId(id);

        validator.valid(authorUpdate);

        return repository.save(authorUpdate);
    }

    @Transactional
    public void delete(UUID id) {
        Author author = getForId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor com id: " + id + " não encontrado!"));

        if (!author.getBooks().isEmpty()) {
            throw new IllegalArgumentException("Não é possivel deletar autor que possui livros cadastrados" + "Total de livros: " + author.getBooks().size());
        }

        repository.delete(author);
    }

    public static class AuthorStats {
        private final String authorName;
        private final int totalBooks;
        private final long totalQuantityBooksInStock;

        public AuthorStats(String authorName, int totalBooks, long totalQuantityBooksInStock) {
            this.authorName = authorName;
            this.totalBooks = totalBooks;
            this.totalQuantityBooksInStock = totalQuantityBooksInStock;
        }

        public String getAuthorName() {
            return authorName;
        }
        public int getTotalBooks() {
            return totalBooks;
        }
        public long getTotalQuantityBooksInStock() {
            return totalQuantityBooksInStock;
        }
    }
}

