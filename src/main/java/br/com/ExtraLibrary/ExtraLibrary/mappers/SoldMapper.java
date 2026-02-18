package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.SoldRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.SoldResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SoldMapper {

    private final BookRepository repository;

    public SoldMapper(BookRepository repository) {
        this.repository = repository;
    }

    public static Sold toEntity(SoldRequest soldRequest) {
        Customer customer = new Customer();
        customer.setId(soldRequest.customerId());

        return Sold.builder()
                .customer(customer)
                .bookIds(soldRequest.bookIds())
                .discount(soldRequest.discount() != null ? soldRequest.discount() : BigDecimal.ZERO)
                .formPayment(soldRequest.formPayment())
                .build();
    }

    public SoldResponse toDTO(Sold sold) {
        List<SoldResponse.BookSummary> bookSummaries = sold.getBookIds().stream()
                .map(this::createBookSummary)
                .toList();

        return new SoldResponse(
                sold.getId(),
                sold.getCustomer().getId(),
                sold.getCustomer().getName(),
                sold.getCustomer().getEmail(),
                bookSummaries,
                sold.getSubtotal(),
                sold.getDiscount(),
                sold.getFinalPrice(),
                sold.getFormPayment(),
                sold.getDateSale(),
                sold.getCreatedAt()
        );
    }

    private SoldResponse.BookSummary createBookSummary(UUID bookId) {
        return repository.findById(bookId)
                .map(book -> new SoldResponse.BookSummary(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthor().getName(),
                        book.getPrice()
                ))
                .orElse(new SoldResponse.BookSummary(
                        bookId,
                        "Livro não encontrado",
                        "Autor desconhecido",
                        BigDecimal.ZERO
                ));
    }
}
