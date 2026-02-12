package br.com.ExtraLibrary.ExtraLibrary.validators;

import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.repository.AuthorRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthorValidator {

    private AuthorRepository repository;

    public AuthorValidator(AuthorRepository repository) {
        this.repository = repository;
    }

    public void valid(Author author) {
        if (existAuthor(author)){
            throw new DuplicatedRegisterException("Autor já cadastrado");
        }
    }

    public boolean existAuthor(Author author) {
        Optional<Author> findAuthor = repository.findByNameAndBirthDateAndNacionality(
                author.getName(), author.getBirthDate(), author.getNacionality()
        );

        if (author.getId() == null) {
            return findAuthor.isPresent();
        }
        return !author.getId().equals(findAuthor.get().getId()) && findAuthor.isPresent();
    }
}
