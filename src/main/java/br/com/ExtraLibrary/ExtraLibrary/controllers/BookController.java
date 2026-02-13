package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.BookRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.BookResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.BookMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Book;
import br.com.ExtraLibrary.ExtraLibrary.services.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/book")
@Tag(name = "Books Manager", description = "Manager for register books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create/Register new Books", description = "Endpoint for register Books")
    public ResponseEntity<Object> save(@RequestBody @Valid BookRequest bookRequest) {
        try {
            Book book = BookMapper.toEntity(bookRequest);
            Book bookSaved = service.save(book, bookRequest.authorId());

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(bookSaved.getId())
                    .toUri();

            return ResponseEntity.created(location).build();
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details from id", description = "Endpoint for Get books details ")
    public ResponseEntity<BookResponse> getDetailsFromId(@PathVariable("id") String id) {
        try {
            UUID bookId = UUID.fromString(id);

            return service
                    .getForId(bookId)
                    .map(book -> {
                        BookResponse bookResponse = BookMapper.toDTO(book);
                        return ResponseEntity.ok(bookResponse);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all Details", description = "Endpoint for Get all books details")
    public ResponseEntity<List<BookResponse>> getAll() {
        List<Book> books = service.getAll();
        List<BookResponse> bookResponses = books.stream()
                .map(BookMapper::toDTO)
                .toList();

        return ResponseEntity.ok(bookResponses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updated Books details", description = "Endpoint for update books")
    public ResponseEntity<Object> update(@PathVariable("id") String id, @RequestBody @Valid BookRequest bookRequest) {
        try {
            UUID bookId = UUID.fromString(id);
            Book book = BookMapper.toEntity(bookRequest);
            service.update(bookId, book, bookRequest.authorId());

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PatchMapping("/{id}/addQuantity")
    @Operation(summary = "AddStock", description = "add books in stock")
    public ResponseEntity<Object> addQuantity(@Parameter(description = "Book ID", required = true) @PathVariable("id") String id, @RequestParam("quantity") @Min(value = 1, message = "Deve ser maior que zero") Long quantity) {
        try {
            UUID bookId = UUID.fromString(id);
            service.addBookStock(bookId, quantity);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PatchMapping("/{id}/sellBook")
    @Operation(summary = "SellBooks", description = "Endpoint for sell/remove quantity books in stock")
    public ResponseEntity<Object> sellBooks(@Parameter(description = "Book ID", required = true) @PathVariable("id") String id, @RequestParam("quantity") @Min(value = 1, message = "Deve ser maior que zero") Long quantity) {
        try {
            UUID bookId = UUID.fromString(id);
            service.sellBookStock(bookId, quantity);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book", description = "Endpoint for delete book")
    public ResponseEntity<Object> delete(@PathVariable("id") String id) {
        try {
            UUID bookId = UUID.fromString(id);
            service.delete(bookId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}
