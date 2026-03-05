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
@Tag(name = "Authentication", description = "Endpoints de Autenticação - Login e Registro de Usuários")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registrar Novo Usuário",
            description = "Registra um novo cliente no sistema com senha criptografada. " +
                    "Após o registro, o usuário recebe um token JWT e pode realizar compras imediatamente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Usuário registrado com sucesso - Token JWT retornado"),
            @ApiResponse(responseCode = "409", description = "❌ Email ou CPF já está em uso"),
            @ApiResponse(responseCode = "400", description = "❌ Dados inválidos")
    })
    public ResponseEntity<Object> register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            logger.info("🆕 Registration attempt from IP: {} for email: {}", clientIp, request.email());

            AuthResponse response = authenticationService.register(request);

            logger.info("✅ User successfully registered: {} (ID: {}) from IP: {}",
                    response.email(), response.userId(), clientIp);

            // Resposta enriquecida mantendo a estrutura original
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuário registrado com sucesso! Você já pode fazer compras.",
                    "authData", Map.of(
                            "token", response.token(),
                            "type", response.type(),
                            "userId", response.userId(),
                            "email", response.email(),
                            "name", response.name(),
                            "role", response.role()
                    ),
                    "userInfo", Map.of(
                            "registrationTime", LocalDateTime.now().toString(),
                            "clientIp", clientIp,
                            "status", "ACTIVE"
                    ),
                    "instructions", Map.of(
                            "usage", "Use o token no header: Authorization: Bearer " + response.token().substring(0, 20) + "...",
                            "access", "Agora você pode acessar os endpoints de vendas",
                            "expires", "Token válido por 24 horas",
                            "role", "Seu perfil: " + response.role()
                    )
            ));

        } catch (DuplicatedRegisterException e) {
            logger.warn("⚠️ Registration failed - duplicate data: {}", e.getMessage());
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("❌ Registration failed for email {}: {}", request.email(), e.getMessage());
            var error = ErrorResponse.badrequest("Erro ao registrar usuário: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Fazer Login",
            description = "Autentica o usuário e retorna um token JWT. " +
                    "O token deve ser usado no header `Authorization: Bearer <token>` " +
                    "para acessar endpoints protegidos como vendas, consultas, etc."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Login realizado com sucesso - Token JWT retornado"),
            @ApiResponse(responseCode = "401", description = "❌ Email ou senha incorretos"),
            @ApiResponse(responseCode = "400", description = "❌ Dados de entrada inválidos")
    })
    public ResponseEntity<Object> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            logger.info("🔑 Login attempt from IP: {} for email: {}", clientIp, request.email());

            AuthResponse response = authenticationService.authenticate(request);

            logger.info("✅ User successfully logged in: {} (ID: {}, Role: {}) from IP: {}",
                    response.email(), response.userId(), response.role(), clientIp);

            // Resposta enriquecida mantendo a estrutura original
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login realizado com sucesso! Você pode fazer compras agora.",
                    "authData", Map.of(
                            "token", response.token(),
                            "type", response.type(),
                            "userId", response.userId(),
                            "email", response.email(),
                            "name", response.name(),
                            "role", response.role()
                    ),
                    "sessionInfo", Map.of(
                            "loginTime", LocalDateTime.now().toString(),
                            "clientIp", clientIp,
                            "userAgent", userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) + "..." : "Unknown",
                            "sessionActive", true
                    ),
                    "instructions", Map.of(
                            "usage", "Copie o token e use no header: Authorization: Bearer <token>",
                            "endpoints", "Agora você pode acessar /api/v1/sold/* para fazer compras",
                            "expires", "Token válido por 24 horas",
                            "permissions", "Perfil de acesso: " + response.role()
                    )
            ));

        } catch (BadCredentialsException e) {
            logger.warn("⚠️ Login failed - invalid credentials for email: {}", request.email());
            var error = ErrorResponse.unauthorized("❌ Email ou senha incorretos");
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("❌ Login failed for email {}: {}", request.email(), e.getMessage());
            var error = ErrorResponse.badrequest("Erro ao fazer login: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Fazer Logout",
            description = "Desloga o usuário atual. O token JWT deve ser removido no frontend após esta operação. " +
                    "Importante para permitir login de outro usuário com segurança."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "✅ Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "❌ Token JWT inválido ou ausente")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication, HttpServletRequest request) {
        try {
            String userEmail = authentication != null ? authentication.getName() : "Usuário não identificado";
            String authorities = authentication != null ? authentication.getAuthorities().toString() : "[]";
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);

            logger.info("🚪 User {} (roles: {}) logged out from IP: {}", userEmail, authorities, clientIp);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Logout realizado com sucesso! Você pode fazer login com outro usuário.",
                    "sessionInfo", Map.of(
                            "user", userEmail,
                            "authorities", authorities,
                            "logoutTime", LocalDateTime.now().toString(),
                            "clientIp", clientIp
                    ),
                    "instructions", Map.of(
                            "frontend", "❗ Remova o token do localStorage/sessionStorage",
                            "redirect", "🔄 Redirecione para a tela de login",
                            "security", "🔒 O token não será mais válido no cliente",
                            "nextStep", "Faça login novamente para acessar endpoints protegidos"
                    )
            ));

        } catch (Exception e) {
            logger.error("❌ Error during logout process: {}", e.getMessage());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout processado com segurança",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
}
