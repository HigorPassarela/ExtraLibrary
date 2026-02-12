package br.com.ExtraLibrary.ExtraLibrary.validators;

import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BookValidator {

    private BookRepository repository;

    public BookValidator(BookRepository repository) {
        this.repository = repository;
    }

    public void valid(Book book) {
        if (existBook(book)){
            throw new DuplicatedRegisterException("Livro já cadastrado");
        }
    }

    public boolean existBook(Book book) {
        Optional<Book> findBook = repository.findByIsbnAndTitleAndPublicationDateAndGenderAndPriceAndBookStatus(
                book.getIsbn(), book.getTitle(), book.getPublicationDate(), book.getGender(), book.getPrice(), book.getBookStatus()
        );

        if (book.getId() == null) {
            return findBook.isPresent();
        }
        return findBook.isPresent() && !findBook.get().getId().equals(book.getId());
    }
}
