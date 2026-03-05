package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookGender;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    // Método original
    Optional<Book> findByIsbnAndTitleAndPublicationDateAndGenderAndPriceAndBookStatus(
            String isbn, String title, LocalDate publicationDate,
            BookGender gender, BigDecimal price, BookStatus bookStatus);

    // Busca por ISBN
    Optional<Book> findByIsbn(String isbn);

    // Busca por título (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Busca por autor
    List<Book> findByAuthorId(UUID authorId);

    // Busca por gênero
    List<Book> findByGender(BookGender gender);

    // Busca por status
    List<Book> findByBookStatus(BookStatus status);

    // Busca por quantidade menor que
    List<Book> findByQuantityLessThan(Long quantity);

    // Busca livros disponíveis ordenados por título
    @Query("SELECT b FROM Book b WHERE b.quantity > 0 ORDER BY b.title")
    List<Book> findAvailableBooksOrderByTitle();

    // Contadores para relatórios
    long countByBookStatus(BookStatus status);

    long countByQuantityLessThan(Long quantity);

    // Soma do valor total do inventário
    @Query("SELECT SUM(b.price * b.quantity) FROM Book b")
    BigDecimal sumTotalInventoryValue();

    // Busca por faixa de preço
    @Query("SELECT b FROM Book b WHERE b.price BETWEEN :minPrice AND :maxPrice")
    List<Book> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // Busca livros publicados em um período
    @Query("SELECT b FROM Book b WHERE b.publicationDate BETWEEN :startDate AND :endDate")
    List<Book> findByPublicationDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Top livros mais caros
    @Query("SELECT b FROM Book b ORDER BY b.price DESC")
    List<Book> findTopExpensiveBooks();

    // Livros de um autor específico em estoque
    @Query("SELECT b FROM Book b WHERE b.author.id = :authorId AND b.quantity > 0")
    List<Book> findAvailableBooksByAuthor(@Param("authorId") UUID authorId);

    // Contagem de livros por gênero
    @Query("SELECT b.gender, COUNT(b) FROM Book b GROUP BY b.gender")
    List<Object[]> countBooksByGender();

    // Valor total por status
    @Query("SELECT b.bookStatus, SUM(b.price * b.quantity) FROM Book b GROUP BY b.bookStatus")
    List<Object[]> sumValueByStatus();
}
