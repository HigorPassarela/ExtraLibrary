package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {

    // Seus métodos existentes
    Optional<Author> findByNameAndBirthDateAndNacionality(String name, LocalDate birthDate, String nacionality);

    List<Author> findByName(String name);

    List<Author> findByNacionality(String nacionality);

    // Busca case-insensitive por nome
    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Author> findByNameContainingIgnoreCase(String name);

    // Busca case-insensitive por nacionalidade
    @Query("SELECT a FROM Author a WHERE LOWER(a.nacionality) = LOWER(:nacionality)")
    List<Author> findByNacionalityIgnoreCase(String nacionality);

    // Autores com mais livros (para relatórios)
    @Query("SELECT a FROM Author a WHERE SIZE(a.books) > 0 ORDER BY SIZE(a.books) DESC")
    List<Author> findAuthorsWithMostBooks();

    // Autores sem livros
    @Query("SELECT a FROM Author a WHERE SIZE(a.books) = 0")
    List<Author> findAuthorsWithoutBooks();

    // Contar autores por nacionalidade
    @Query("SELECT a.nacionality, COUNT(a) FROM Author a GROUP BY a.nacionality ORDER BY COUNT(a) DESC")
    List<Object[]> countAuthorsByNacionality();

    // Contar autores com livros
    @Query("SELECT COUNT(a) FROM Author a WHERE SIZE(a.books) > 0")
    long countAuthorsWithBooks();
}
