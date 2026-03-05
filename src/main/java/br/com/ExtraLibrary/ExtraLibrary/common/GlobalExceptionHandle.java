package br.com.ExtraLibrary.ExtraLibrary.common;

import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorField;
import br.com.ExtraLibrary.ExtraLibrary.dto.error.ErrorResponse;
import br.com.ExtraLibrary.ExtraLibrary.exception.DuplicatedRegisterException;
import br.com.ExtraLibrary.ExtraLibrary.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandle {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandle.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.warn("Validation error in request to {}: {}", request.getDescription(false), ex.getMessage());

        List<ErrorField> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorField(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.validationError(
                "Dados de entrada inválidos",
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        logger.warn("Resource not found in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.notFound(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(DuplicatedRegisterException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatedRegister(
            DuplicatedRegisterException ex, WebRequest request) {

        logger.warn("Duplicate register attempt in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.conflict(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        logger.warn("Illegal argument in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.badrequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {

        logger.warn("Bad credentials in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.unauthorized("Credenciais inválidas");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {

        logger.warn("Authentication error in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.unauthorized("Falha na autenticação");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        logger.warn("Access denied in request to {}: {}", request.getDescription(false), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.forbidden("Acesso negado");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("Unexpected error in request to {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.internalServerError(
                "Erro interno do servidor. Tente novamente mais tarde."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
