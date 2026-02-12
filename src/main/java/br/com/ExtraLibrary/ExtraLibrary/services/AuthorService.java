package br.com.ExtraLibrary.ExtraLibrary.services;

import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.repository.AuthorRepository;
import br.com.ExtraLibrary.ExtraLibrary.validators.AuthorValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

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
}
