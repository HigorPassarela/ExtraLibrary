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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("api/v1/author")
@Tag(name = "Author Manager", description = "Manager for register Authors")
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create/Register new Author", description = "Endpoint for register Authors")
    public ResponseEntity<Object> save(@RequestBody @Valid AuthorRequest authorRequest) {
        try {
            Author author = AuthorMapper.toEntity(authorRequest);
            service.save(author);

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(author.getId())
                    .toUri();

            return ResponseEntity.created(location).build();
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update author", description = "Endpoint for updated stats from specific author")
    public ResponseEntity<Object> update(@Parameter(description = "Author ID", required = true) @PathVariable("id") String id, @RequestBody @Valid AuthorRequest authorRequest) {
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

    @GetMapping("{id}")
    @Operation(summary = "Get details from id", description = "Endpoint for Get authors details ")
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

    @GetMapping
    @Operation(summary = "List all authors", description = "Endpoint for list all authors")
    public ResponseEntity<List<AuthorResponse>> getAll() {
        List<Author> authors = service.getAll();
        List<AuthorResponse> authorResponses = authors.stream()
                .map(AuthorMapper::toDTO)
                .toList();

        return ResponseEntity.ok(authorResponses);
    }

    @GetMapping("/name")
    @Operation(summary = "Find authors from name", description = "Endpoint for list authors from your names")
    public ResponseEntity<List<AuthorResponse>> findByName(@Parameter(description = "Name", required = true) @RequestParam("name") String name) {
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

    @GetMapping("/nacionality")
    @Operation(summary = "Find authors from nacionality", description = "Endpoint for list authors from your nacionality")
    public ResponseEntity<List<AuthorResponse>> findByNacionality(@Parameter(description = "Nacionality", required = true) @RequestParam("nacionality") String nacionality) {
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

    @GetMapping("/{id}/stats")
    @Operation(summary = "Find authors stats/books from id", description = "Endpoint for list authors stats from id")
    public ResponseEntity<Object> getAuthorStats(@Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
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

    @GetMapping("/{id}/books")
    @Operation(summary = "Find book of author", description = "Find all books from specific author")
    public ResponseEntity<AuthorResponse> getAuthorBooks(@Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete author", description = "Delete specific author from id")
    public ResponseEntity<Object> delete(@Parameter(description = "Author ID", required = true) @PathVariable("id") String id) {
        try {
            UUID authorId = UUID.fromString(id);
            service.delete(authorId);

            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}
