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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/book")
@Tag(name = "Books Management", description = "Gerenciamento Completo de Livros - Catálogo, Estoque e Vendas")
@SecurityRequirement(name = "Bearer Authentication")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar Novo Livro",
            description = "Registra um novo livro no catálogo vinculando-o a um autor existente. " +
                    "Valida ISBN único e dados obrigatórios. Apenas staff autorizado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Autor não encontrado"),
            @ApiResponse(responseCode = "409", description = "ISBN já existe no sistema"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
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
    @Operation(
            summary = "Consultar Livro por ID",
            description = "Retorna detalhes completos de um livro incluindo autor, estoque e preço. " +
                    "Disponível para todos os usuários autenticados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro encontrado"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<BookResponse> getDetailsFromId(
            @Parameter(description = "UUID do livro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Listar Todos os Livros",
            description = "Retorna catálogo completo de livros disponível para navegação. " +
                    "Endpoint público para facilitar busca e descoberta de títulos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo de livros retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente")
    })
    public ResponseEntity<List<BookResponse>> getAll() {
        List<Book> books = service.getAll();
        List<BookResponse> bookResponses = books.stream()
                .map(BookMapper::toDTO)
                .toList();

        return ResponseEntity.ok(bookResponses);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar Dados do Livro",
            description = "Atualiza informações de um livro existente incluindo preço, descrição e autor. " +
                    "Valida integridade dos dados e referências."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Livro atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Livro ou autor não encontrado"),
            @ApiResponse(responseCode = "409", description = "ISBN conflitante"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> update(
            @Parameter(description = "UUID do livro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
            @RequestBody @Valid BookRequest bookRequest) {
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
    @Operation(
            summary = "Adicionar Estoque",
            description = "Adiciona quantidade ao estoque de um livro específico. " +
                    "Usado para reposição de estoque após recebimento de mercadorias."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Estoque adicionado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida (deve ser > 0)")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> addQuantity(
            @Parameter(description = "UUID do livro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
            @Parameter(description = "Quantidade a adicionar", required = true, example = "10")
            @RequestParam("quantity") @Min(value = 1, message = "Deve ser maior que zero") Long quantity) {
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
    @Operation(
            summary = "Registrar Venda (Reduzir Estoque)",
            description = "Remove quantidade do estoque ao registrar uma venda. " +
                    "Valida se há estoque suficiente antes de processar."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Venda processada e estoque atualizado"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente ou quantidade inválida")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> sellBooks(
            @Parameter(description = "UUID do livro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
            @Parameter(description = "Quantidade vendida", required = true, example = "2")
            @RequestParam("quantity") @Min(value = 1, message = "Deve ser maior que zero") Long quantity) {
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
    @Operation(
            summary = "Excluir Livro",
            description = "Remove um livro do catálogo permanentemente. " +
                    "ATENÇÃO: Ação irreversível que pode afetar histórico de vendas. Apenas administradores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Livro excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "400", description = "ID inválido ou livro possui vendas associadas")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> delete(
            @Parameter(description = "UUID do livro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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

    @GetMapping("/search/title")
    @Operation(
            summary = "Buscar por Título",
            description = "Busca livros por título (busca parcial e case-insensitive). " +
                    "Útil para encontrar livros quando você lembra parte do nome."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca por título realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BookResponse>> searchByTitle(
            @Parameter(description = "Título ou parte do título", required = true, example = "Dom Casmurro")
            @RequestParam String title) {
        List<Book> books = service.findByTitle(title);
        List<BookResponse> responses = books.stream()
                .map(BookMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search/isbn/{isbn}")
    @Operation(
            summary = "Buscar por ISBN",
            description = "Busca livro específico pelo código ISBN. " +
                    "Retorna resultado único já que ISBN é identificador exclusivo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livro encontrado pelo ISBN"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "ISBN não encontrado no catálogo")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<BookResponse> searchByIsbn(
            @Parameter(description = "Código ISBN do livro", required = true, example = "978-85-359-0277-5")
            @PathVariable String isbn) {
        return service.findByIsbn(isbn)
                .map(book -> ResponseEntity.ok(BookMapper.toDTO(book)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search/author/{authorId}")
    @Operation(
            summary = "Livros por Autor",
            description = "Retorna todos os livros de um autor específico. " +
                    "Ideal para explorar a obra completa de um escritor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livros do autor retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "ID do autor inválido")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String authorId) {
        try {
            UUID authorUuid = UUID.fromString(authorId);
            List<Book> books = service.findByAuthor(authorUuid);
            List<BookResponse> responses = books.stream()
                    .map(BookMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search/genre/{genre}")
    @Operation(
            summary = "Livros por Gênero",
            description = "Filtra livros por categoria/gênero literário. " +
                    "Perfeito para descobrir novos títulos em gêneros favoritos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livros do gênero retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "Gênero inválido")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BookResponse>> getBooksByGenre(
            @Parameter(description = "Gênero literário", required = true, example = "Romance")
            @PathVariable String genre) {
        try {
            List<Book> books = service.findByGender(genre);
            List<BookResponse> responses = books.stream()
                    .map(BookMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/available")
    @Operation(
            summary = "Livros Disponíveis",
            description = "Retorna apenas livros com estoque disponível para venda. " +
                    "Filtra automaticamente livros em falta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livros disponíveis retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BookResponse>> getAvailableBooks() {
        List<Book> books = service.findAvailableBooks();
        List<BookResponse> responses = books.stream()
                .map(BookMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/inventory/report")
    @Operation(
            summary = "Relatório de Inventário",
            description = "Gera relatório completo do estoque: total de livros, valores, " +
                    "produtos em falta, etc. Informações gerenciais para controle de estoque."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relatório de inventário gerado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getInventoryReport() {
        Object report = service.getInventoryReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/inventory/low-stock")
    @Operation(
            summary = "Livros com Estoque Baixo",
            description = "Identifica livros com estoque abaixo do limite configurado. " +
                    "Essencial para reposição proativa de mercadorias."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Livros com estoque baixo identificados"),
            @ApiResponse(responseCode = "401", description = "Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - Apenas ADMIN/LIBRARIAN")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BookResponse>> getLowStockBooks(
            @Parameter(description = "Limite mínimo de estoque", example = "5")
            @RequestParam(defaultValue = "5") Long threshold) {
        List<Book> books = service.findLowStockBooks(threshold);
        List<BookResponse> responses = books.stream()
                .map(BookMapper::toDTO)
                .toList();

        return ResponseEntity.ok(responses);
    }
}