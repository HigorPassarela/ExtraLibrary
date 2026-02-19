package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.SoldRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.SoldResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Customer;
import br.com.ExtraLibrary.ExtraLibrary.models.Sold;
import br.com.ExtraLibrary.ExtraLibrary.repository.BookRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
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
                .booksQuantity(new HashMap<>(soldRequest.booksQuantity()))
                .discount(soldRequest.discount() != null ? soldRequest.discount() : BigDecimal.ZERO)
                .formPayment(soldRequest.formPayment())
                .build();
    }

    public SoldResponse toDTO(Sold sold) {
        Map<SoldResponse.BookSummary, Long> booksWithQuantities = new HashMap<>();

        for (Map.Entry<UUID, Long> entry : sold.getBooksQuantity().entrySet()) {
            UUID bookId = entry.getKey();
            Long quantity = entry.getValue();

            SoldResponse.BookSummary bookSummary = createBookSummary(bookId);
            booksWithQuantities.put(bookSummary, quantity);
        }

        return new SoldResponse(
                sold.getId(),
                sold.getCustomer().getId(),
                sold.getCustomer().getName(),
                sold.getCustomer().getEmail(),
                booksWithQuantities, // Agora é Map<BookSummary, Long>
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
