package br.com.ExtraLibrary.ExtraLibrary.mappers;

import br.com.ExtraLibrary.ExtraLibrary.dto.request.AuthorRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthorResponse;
import br.com.ExtraLibrary.ExtraLibrary.models.Author;

public class AuthorMapper {

    public static Author toEntity(AuthorRequest authorRequest) {
        return Author.builder()
                .name(authorRequest.name())
                .biography(authorRequest.biography())
                .nacionality(authorRequest.nacionality())
                .birthDate(authorRequest.birthDate())
                .build();
    }

    public static AuthorResponse toDTO (Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getBiography(),
                author.getNacionality(),
                author.getBirthDate()
        );
    }
}
