package br.com.ExtraLibrary.ExtraLibrary.dto.response;

import java.util.UUID;

public record AuthResponse(
        String token,
        String type,
        UUID userId,
        String email,
        String name,
        String role
) {}
