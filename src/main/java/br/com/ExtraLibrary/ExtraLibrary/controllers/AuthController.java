package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.LoginRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.RegisterRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.services.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticação de usuários")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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

    // NOVO MÉTODO DE LOGOUT (SIMPLES)
    @PostMapping("/logout")
    @Operation(
            summary = "Logout do usuário",
            description = "Desloga o usuário atual para permitir login de outro usuário. O token deve ser removido no frontend."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication, HttpServletRequest request) {
        try {
            // Pegar informações do usuário autenticado
            String userEmail = authentication != null ? authentication.getName() : "Usuário não identificado";
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);

            // Log do logout para auditoria
            logger.info("User {} logged out from IP: {} - UserAgent: {}", userEmail, clientIp, userAgent);

            // Resposta de sucesso com instruções para o frontend
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout realizado com sucesso! Você pode fazer login com outro usuário.",
                    "user", userEmail,
                    "timestamp", LocalDateTime.now().toString(),
                    "instructions", Map.of(
                            "frontend", "Remova o token do localStorage/sessionStorage",
                            "redirect", "Redirecione para a tela de login",
                            "action", "O token não será mais válido no cliente"
                    )
            ));

        } catch (Exception e) {
            logger.error("Error during logout process: {}", e.getMessage());

            // Mesmo com erro, retornamos sucesso pois o logout é sempre válido
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout processado",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // Método auxiliar para pegar o IP real do cliente
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
}
