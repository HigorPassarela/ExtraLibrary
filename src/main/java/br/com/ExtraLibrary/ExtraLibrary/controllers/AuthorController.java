package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.AuthorRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthorResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import br.com.ExtraLibrary.ExtraLibrary.mappers.AuthorMapper;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;
import br.com.ExtraLibrary.ExtraLibrary.services.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/author")
@Tag(name = "Author Manager", description = "Manager for register Authors")
@SecurityRequirement(name = "Bearer Authentication") // ✅ Requer autenticação
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    // ✅ Criar autor - apenas ADMIN/LIBRARIAN
    @PostMapping
    @Operation(summary = "Create/Register new Author", description = "Endpoint for register Authors - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> save(@RequestBody @Valid AuthorRequest authorRequest) {
        try {
            Author author = AuthorMapper.toEntity(authorRequest);
            Author saved = service.save(author);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location).build();
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Atualizar autor - apenas ADMIN/LIBRARIAN
    @PutMapping("/{id}")
    @Operation(summary = "Update author", description = "Endpoint for updated author details - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> update(
            @Parameter(description = "Author ID", required = true) @PathVariable("id") String id,
            @RequestBody @Valid AuthorRequest authorRequest) {
        try {
            UUID authorId = UUID.fromString(id);
            Author author = AuthorMapper.toEntity(authorRequest);
            service.update(authorId, author);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Buscar autor por ID - todos os usuários autenticados
    @GetMapping("/{id}")
    @Operation(summary = "Get details from id", description = "Endpoint for Get authors details")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<AuthorResponse> getDetailsFromId(@PathVariable("id") String id) {
        try {
            UUID idAuthor = UUID.fromString(id);

            return service
                    .getForId(idAuthor)
                    .map(author -> {
                        AuthorResponse authorResponse = AuthorMapper.toDTO(author);
                        return ResponseEntity.ok(authorResponse);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Listar todos os autores - todos os usuários autenticados
    @GetMapping
    @Operation(summary = "List all authors", description = "Endpoint for list all authors")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<AuthorResponse>> getAll() {
        List<Author> authors = service.getAll();
        List<AuthorResponse> authorResponses = authors.stream()
                .map(AuthorMapper::toDTO)
                .toList();

        return ResponseEntity.ok(authorResponses);
    }

    // ✅ Buscar por nome - todos os usuários autenticados
    @GetMapping("/name")
    @Operation(summary = "Find authors from name", description = "Endpoint for list authors from your names")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<AuthorResponse>> findByName(
            @Parameter(description = "Name", required = true) @RequestParam("name") String name) {
        try {
            List<Author> authors = service.findByName(name);
            List<AuthorResponse> authorResponses = authors.stream()
                    .map(AuthorMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(authorResponses);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Buscar por nacionalidade - todos os usuários autenticados
    @GetMapping("/nacionality")
    @Operation(summary = "Find authors from nacionality", description = "Endpoint for list authors from your nacionality")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<AuthorResponse>> findByNacionality(
            @Parameter(description = "Nacionality", required = true) @RequestParam("nacionality") String nacionality) {
        try {
            List<Author> authors = service.findByNacionality(nacionality);
            List<AuthorResponse> authorResponses = authors.stream()
                    .map(AuthorMapper::toDTO)
                    .toList();

            return ResponseEntity.ok(authorResponses);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Estatísticas do autor - apenas ADMIN/LIBRARIAN
    @GetMapping("/{id}/stats")
    @Operation(summary = "Find authors stats/books from id", description = "Endpoint for list authors stats from id - ADMIN/LIBRARIAN only")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getAuthorStats(
            @Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
        try {
            UUID authorId = UUID.fromString(id);
            AuthorService.AuthorStats stats = service.getAuthorStats(authorId);

            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    // ✅ Livros do autor - todos os usuários autenticados
    @GetMapping("/{id}/books")
    @Operation(summary = "Find books of author", description = "Find all books from specific author")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<AuthorResponse> getAuthorBooks(
            @Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
        try {
            UUID authorId = UUID.fromString(id);

            return service
                    .getForId(authorId)
                    .map(author -> {
                        AuthorResponse authorResponse = AuthorMapper.toDTO(author);
                        return ResponseEntity.ok(authorResponse);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ✅ Deletar autor - apenas ADMIN
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete author", description = "Delete specific author from id - ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> delete(
            @Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
        try {
            UUID authorId = UUID.fromString(id);
            service.delete(authorId);

            return ResponseEntity.noContent().build(); // ✅ Corrigido: deve retornar 204 No Content
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}
