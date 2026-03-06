package br.com.ExtraLibrary.ExtraLibrary.controllers;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.LoginRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.request.RegisterRequest;
import br.com.ExtraLibrary.ExtraLibrary.dto.response.AuthResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.services.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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
@Tag(name = "Authentication System",
        description = "Sistema Completo de Autenticação JWT - Registro, Login e Logout Seguro")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Criar Nova Conta",
            description = "Registra um novo usuário no sistema com validação completa e criptografia de senha. " +
                    "Após o cadastro bem-sucedido, o usuário recebe automaticamente um token JWT válido " +
                    "e pode começar a usar o sistema imediatamente (fazer compras, consultar livros, etc.). " +
                    "\n\nValidações aplicadas:\n" +
                    "• Email único no sistema\n" +
                    "• CPF único e válido\n" +
                    "• Senha criptografada com BCrypt\n" +
                    "• Dados obrigatórios preenchidos\n" +
                    "\nFluxo pós-registro:\n" +
                    "1. Usuário criado com status ACTIVE\n" +
                    "2. Token JWT gerado automaticamente\n" +
                    "3. Pronto para fazer login e compras"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário registrado com sucesso - Token JWT gerado e pronto para uso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Registro bem-sucedido",
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Usuário registrado com sucesso! Você já pode fazer compras.",
                                      "authData": {
                                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "type": "Bearer",
                                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                                        "email": "usuario@email.com",
                                        "name": "João Silva",
                                        "role": "CUSTOMER"
                                      },
                                      "userInfo": {
                                        "registrationTime": "2024-01-15T10:30:00",
                                        "clientIp": "192.168.1.100",
                                        "status": "ACTIVE"
                                      },
                                      "instructions": {
                                        "usage": "Use o token no header: Authorization: Bearer eyJhbGci...",
                                        "access": "Agora você pode acessar os endpoints de vendas",
                                        "expires": "Token válido por 24 horas",
                                        "role": "Seu perfil: CUSTOMER"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de dados - Email ou CPF já cadastrado no sistema",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Dados duplicados",
                                    value = """
                                    {
                                      "status": 409,
                                      "message": "Email já está em uso por outro usuário",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos - Verifique os campos obrigatórios e formatos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validação falhou",
                                    value = """
                                    {
                                      "status": 400,
                                      "message": "CPF inválido ou email mal formatado",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
    public ResponseEntity<Object> register(
            @Parameter(
                    description = "Dados para criação da conta",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Exemplo de registro",
                                    value = """
                                    {
                                      "name": "João Silva",
                                      "email": "joao.silva@email.com",
                                      "cpf": "12345678901",
                                      "password": "minhasenha123",
                                      "phone": "(11) 99999-9999",
                                      "address": "Rua das Flores, 123"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            logger.info("Registration attempt from IP: {} for email: {}", clientIp, request.email());

            AuthResponse response = authenticationService.register(request);

            logger.info("User successfully registered: {} (ID: {}) from IP: {}",
                    response.email(), response.userId(), clientIp);

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
            logger.warn("Registration failed - duplicate data: {}", e.getMessage());
            var error = ErrorResponse.conflict(e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Registration failed for email {}: {}", request.email(), e.getMessage());
            var error = ErrorResponse.badrequest("Erro ao registrar usuário: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "Entrar no Sistema",
            description = "Autentica usuário existente e gera token JWT para acesso aos recursos protegidos. " +
                    "O token retornado deve ser incluído no cabeçalho 'Authorization: Bearer <token>' " +
                    "em todas as requisições para endpoints que exigem autenticação. " +
                    "\n\nProcesso de autenticação:\n" +
                    "1. Valida email e senha\n" +
                    "2. Verifica se usuário está ativo\n" +
                    "3. Gera token JWT válido por 24h\n" +
                    "4. Registra log de acesso com IP\n" +
                    "\nEndpoints liberados após login:\n" +
                    "• /api/v1/sold/* - Sistema de vendas\n" +
                    "• /api/v1/book/* - Catálogo de livros\n" +
                    "• /api/v1/author/* - Consulta de autores\n" +
                    "• /api/v1/customer/profile - Perfil pessoal"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login realizado com sucesso - Token JWT válido gerado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Login bem-sucedido",
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Login realizado com sucesso! Você pode fazer compras agora.",
                                      "authData": {
                                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsInJvbGUiOiJDVVNUT01FUiIsImV4cCI6MTY0MjY4MjQwMH0.signature",
                                        "type": "Bearer",
                                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                                        "email": "user@email.com",
                                        "name": "João Silva",
                                        "role": "CUSTOMER"
                                      },
                                      "sessionInfo": {
                                        "loginTime": "2024-01-15T10:30:00",
                                        "clientIp": "192.168.1.100",
                                        "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit...",
                                        "sessionActive": true
                                      },
                                      "instructions": {
                                        "usage": "Copie o token e use no header: Authorization: Bearer <token>",
                                        "endpoints": "Agora você pode acessar /api/v1/sold/* para fazer compras",
                                        "expires": "Token válido por 24 horas",
                                        "permissions": "Perfil de acesso: CUSTOMER"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciais inválidas - Email ou senha incorretos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Credenciais inválidas",
                                    value = """
                                    {
                                      "status": 401,
                                      "message": "Email ou senha incorretos",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos - Formato de email ou campos obrigatórios"
            )
    })
    public ResponseEntity<Object> login(
            @Parameter(
                    description = "Credenciais de acesso",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Exemplo de login",
                                    value = """
                                    {
                                      "email": "usuario@email.com",
                                      "password": "minhasenha123"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            logger.info("Login attempt from IP: {} for email: {}", clientIp, request.email());

            AuthResponse response = authenticationService.authenticate(request);

            logger.info("User successfully logged in: {} (ID: {}, Role: {}) from IP: {}",
                    response.email(), response.userId(), response.role(), clientIp);

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
            logger.warn("Login failed - invalid credentials for email: {}", request.email());
            var error = ErrorResponse.unauthorized("Email ou senha incorretos");
            return ResponseEntity.status(error.status()).body(error);
        } catch (Exception e) {
            logger.error("Login failed for email {}: {}", request.email(), e.getMessage());
            var error = ErrorResponse.badrequest("Erro ao fazer login: " + e.getMessage());
            return ResponseEntity.status(error.status()).body(error);
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Sair do Sistema",
            description = "Finaliza a sessão do usuário de forma segura e registra o logout para auditoria. " +
                    "Após esta operação, o token JWT deve ser removido do frontend (localStorage/sessionStorage) " +
                    "para garantir que outro usuário possa fazer login com segurança. " +
                    "\n\nProcesso de logout seguro:\n" +
                    "1. Registra horário e IP do logout\n" +
                    "2. Invalida a sessão no cliente\n" +
                    "3. Orienta remoção do token\n" +
                    "4. Permite novo login imediato\n" +
                    "\nImportante para segurança:\n" +
                    "• Remover token do armazenamento local\n" +
                    "• Redirecionar para tela de login\n" +
                    "• Limpar dados sensíveis da memória"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout realizado com sucesso - Sessão finalizada com segurança",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Logout bem-sucedido",
                                    value = """
                                    {
                                      "success": true,
                                      "message": "Logout realizado com sucesso! Você pode fazer login com outro usuário.",
                                      "sessionInfo": {
                                        "user": "usuario@email.com",
                                        "authorities": "[ROLE_CUSTOMER]",
                                        "logoutTime": "2024-01-15T10:45:00",
                                        "clientIp": "192.168.1.100"
                                      },
                                      "instructions": {
                                        "frontend": "Remova o token do localStorage/sessionStorage",
                                        "redirect": "Redirecione para a tela de login",
                                        "security": "O token não será mais válido no cliente",
                                        "nextStep": "Faça login novamente para acessar endpoints protegidos"
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT inválido ou ausente - Usuário não autenticado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Token inválido",
                                    value = """
                                    {
                                      "status": 401,
                                      "message": "Token JWT inválido ou expirado",
                                      "timestamp": "2024-01-15T10:30:00"
                                    }
                                    """
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> logout(
            Authentication authentication,
            HttpServletRequest request) {
        try {
            String userEmail = authentication != null ? authentication.getName() : "Usuário não identificado";
            String authorities = authentication != null ? authentication.getAuthorities().toString() : "[]";
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);

            logger.info("User {} (roles: {}) logged out from IP: {}", userEmail, authorities, clientIp);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout realizado com sucesso! Você pode fazer login com outro usuário.",
                    "sessionInfo", Map.of(
                            "user", userEmail,
                            "authorities", authorities,
                            "logoutTime", LocalDateTime.now().toString(),
                            "clientIp", clientIp
                    ),
                    "instructions", Map.of(
                            "frontend", "Remova o token do localStorage/sessionStorage",
                            "redirect", "Redirecione para a tela de login",
                            "security", "O token não será mais válido no cliente",
                            "nextStep", "Faça login novamente para acessar endpoints protegidos"
                    )
            ));

        } catch (Exception e) {
            logger.error("Error during logout process: {}", e.getMessage());

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