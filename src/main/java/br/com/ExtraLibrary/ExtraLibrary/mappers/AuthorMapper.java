package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.AuthorRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthorResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;

import java.util.Collections;

public class AuthorMapper {

    public static Author toEntity(AuthorRequest authorRequest) {
        return Author.builder()
                .name(authorRequest.name())
                .biography(authorRequest.biography())
                .nacionality(authorRequest.nacionality()) // ✅ Mantendo seu campo "nacionality"
                .birthDate(authorRequest.birthDate())
                .build();
    }

    public static AuthorResponse toDTO(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getBiography(),
                author.getNacionality(),
                author.getBirthDate(),
                author.getBooks() != null ? author.getBooks().size() : 0,
                // Se quiser incluir os livros, descomente a linha abaixo:
                // author.getBooks() != null ? author.getBooks().stream().map(BookMapper::toDTO).toList() : Collections.emptyList()
                Collections.emptyList() // ✅ Por enquanto, lista vazia para evitar referência circular
        );
    }
}
