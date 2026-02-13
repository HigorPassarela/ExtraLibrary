package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.BookRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.BookResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;

public class BookMapper {

    public static Book toEntity(BookRequest bookRequest) {
        return Book.builder()
                .isbn(bookRequest.isbn())
                .title(bookRequest.title())
                .publicationDate(bookRequest.publicationDate())
                .gender(bookRequest.gender())
                .price(bookRequest.price())
                .quantity(bookRequest.quantity())
                .build();
    }

    public static BookResponse toDTO (Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getPublicationDate(),
                book.getGender(),
                book.getPrice(),
                book.getQuantity(),
                book.getBookStatus()
        );
    }
}
