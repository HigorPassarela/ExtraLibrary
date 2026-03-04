package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.LoginRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.RegisterRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticação de usuários")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", description = "Registra um novo cliente no sistema com senha criptografada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "409", description = "Email ou CPF já está em uso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<Object> register(@RequestBody @Valid RegisterRequest request) {
        try {
            AuthResponse response = authenticationService.register(request);
            return ResponseEntity.ok(response);
        } catch (DuplicatedRegisterException e) {
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            var error = ErrorResponse.badrequest("Erro ao registrar usuário: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica o usuário e retorna um token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<Object> login(@RequestBody @Valid LoginRequest request) {
        try {
            AuthResponse response = authenticationService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            var error = ErrorResponse.badrequest("Credenciais inválidas");
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            var error = ErrorResponse.badrequest("Erro ao fazer login: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }
}
