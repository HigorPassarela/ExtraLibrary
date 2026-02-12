package br.com.ExtraLibrary.ExtraLibrary.repository;

import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookGender;
import br.com.ExtraLibrary.ExtraLibrary.models.enums.BookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbnAndTitleAndPublicationDateAndGenderAndPriceAndBookStatus(String isbn, String title, LocalDate publicationDate, BookGender gender, BigDecimal price, BookStatus bookStatus);

}
