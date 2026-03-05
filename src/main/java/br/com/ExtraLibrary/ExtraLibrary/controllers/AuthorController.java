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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "📖 Author Management", description = "Gerenciamento de Autores - CRUD completo e consultas especializadas")
@SecurityRequirement(name = "Bearer Authentication")
public class AuthorController {

    private final AuthorService service;

    public AuthorController(AuthorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar Novo Autor",
            description = "Registra um novo autor no sistema com validação de duplicatas. " +
                    "Apenas administradores e bibliotecários podem cadastrar autores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "✅ Autor cadastrado com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "❌ Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "409", description = "❌ Autor já existe com esses dados"),
            @ApiResponse(responseCode = "400", description = "❌ Dados inválidos")
    })
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar Dados do Autor",
            description = "Atualiza informações de um autor existente. " +
                    "Valida duplicatas e verifica se o autor existe antes de atualizar."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "✅ Autor atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "❌ Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "❌ Autor não encontrado"),
            @ApiResponse(responseCode = "409", description = "❌ Dados conflitantes com outro autor"),
            @ApiResponse(responseCode = "400", description = "❌ ID inválido ou dados incorretos")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> update(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
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

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar Autor por ID",
            description = "Retorna detalhes completos de um autor específico incluindo seus livros. " +
                    "Disponível para todos os usuários autenticados."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Autor encontrado"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "❌ Autor não encontrado"),
            @ApiResponse(responseCode = "400", description = "❌ ID inválido")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<AuthorResponse> getDetailsFromId(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Listar Todos os Autores",
            description = "Retorna lista completa de todos os autores cadastrados no sistema. " +
                    "Endpoint público para facilitar navegação e busca de livros."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Lista de autores retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente")
    })
    public ResponseEntity<List<AuthorResponse>> getAll() {
        List<Author> authors = service.getAll();
        List<AuthorResponse> authorResponses = authors.stream()
                .map(AuthorMapper::toDTO)
                .toList();

        return ResponseEntity.ok(authorResponses);
    }

    @GetMapping("/name")
    @Operation(
            summary = "Buscar Autores por Nome",
            description = "Busca autores por nome (busca parcial). " +
                    "Útil para encontrar autores quando você não sabe o nome completo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Busca realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "❌ Parâmetro de busca inválido")
    })
    public ResponseEntity<List<AuthorResponse>> findByName(
            @Parameter(description = "Nome ou parte do nome do autor", required = true, example = "Machado")
            @RequestParam("name") String name) {
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
    @Operation(
            summary = "Buscar Autores por Nacionalidade",
            description = "Filtra autores por nacionalidade. " +
                    "Útil para encontrar literatura de países específicos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Busca por nacionalidade realizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "400", description = "❌ Nacionalidade inválida")
    })
    public ResponseEntity<List<AuthorResponse>> findByNacionality(
            @Parameter(description = "Nacionalidade do autor", required = true, example = "Brasileira")
            @RequestParam("nacionality") String nacionality) {
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
    @Operation(
            summary = "Estatísticas do Autor",
            description = "Retorna estatísticas detalhadas do autor: quantidade de livros, vendas, etc. " +
                    "Informações gerenciais para administradores e bibliotecários."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Estatísticas retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "❌ Acesso negado - Apenas ADMIN/LIBRARIAN"),
            @ApiResponse(responseCode = "404", description = "❌ Autor não encontrado"),
            @ApiResponse(responseCode = "400", description = "❌ ID inválido")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Object> getAuthorStats(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Livros do Autor",
            description = "Retorna todos os livros de um autor específico. " +
                    "Útil para clientes navegarem pela obra completa de um autor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Livros do autor retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "404", description = "❌ Autor não encontrado"),
            @ApiResponse(responseCode = "400", description = "❌ ID inválido")
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'LIBRARIAN')")
    public ResponseEntity<AuthorResponse> getAuthorBooks(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
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
    @Operation(
            summary = "Excluir Autor",
            description = "Remove um autor do sistema permanentemente. " +
                    "⚠️ **ATENÇÃO**: Esta ação é irreversível e pode afetar livros associados. Apenas administradores."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "✅ Autor excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente"),
            @ApiResponse(responseCode = "403", description = "❌ Acesso negado - Apenas ADMIN"),
            @ApiResponse(responseCode = "404", description = "❌ Autor não encontrado"),
            @ApiResponse(responseCode = "400", description = "❌ ID inválido ou autor possui livros associados")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> delete(
            @Parameter(description = "UUID do autor", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {
        try {
            UUID authorId = UUID.fromString(id);
            service.delete(authorId);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            var error = ErrorResponse.badrequest(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (ResourceNotFoundException e) {
            var error = ErrorResponse.notFound(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}
