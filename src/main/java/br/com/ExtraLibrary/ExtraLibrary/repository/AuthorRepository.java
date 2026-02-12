package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {

    Optional<Author> findByNameAndBirthDateAndNacionality(String name, LocalDate birthDate, String nacionality);
}
