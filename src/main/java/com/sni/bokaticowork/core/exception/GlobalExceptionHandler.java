package com.sni.bokaticowork.core.exception;

import com.sni.bokaticowork.core.exception.customs.*;
import com.sni.bokaticowork.core.utils.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Jackson 3 · Spring Boot 4 expose tools.jackson, et non com.fasterxml.jackson.
// La classe homonyme de Jackson 2 reste sur le classpath : l'importer ferait echouer
// le instanceof en silence, sans la moindre erreur de compilation.
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponse errorResponse;

    // ── Business / domain exceptions ─────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ResourceAlreadyExistException.class)
    public ResponseEntity<Object> handleResourceAlreadyExists(ResourceAlreadyExistException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.RESOURCE_ALREADY_EXISTS, HttpStatus.CONFLICT,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        ApiError error = errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex, null);
        error.setReason(ex.getReason());
        return respond(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, HttpServletRequest request) {
        ApiError error = errorResponse.build(ErrorCode.CONFLICT, HttpStatus.CONFLICT, ex.getMessage(), request, ex, null);
        error.setReason(ex.getReason());
        return respond(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidation(ValidationException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                ex.getMessage(), request, ex, ex.getErrors()));
    }

    // ── Auth / access exceptions ──────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                ex.getMessage(), request, ex, null));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Access to this resource is forbidden.", request, ex, null));
    }

    private com.sni.bokaticowork.core.audit.security.SecurityEvents securityEvents;

    /** Lot A2 : evenements de securite (facultatif dans les tests). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setSecurityEvents(com.sni.bokaticowork.core.audit.security.SecurityEvents securityEvents) {
        this.securityEvents = securityEvents;
    }

    @ExceptionHandler(AccesDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccesDeniedException ex, HttpServletRequest request) {
        if (securityEvents != null) {
            securityEvents.accessDenied(request, "SERVICE");
        }
        return respond(errorResponse.build(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
                "Access denied.", request, ex, null));
    }

    /**
     * Le refus d'une methode protegee ({@code @PreAuthorize}) · il traverse le controleur, le filtre
     * de securite ne le voit donc pas, et sans cette regle il finissait en 500.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleMethodAccessDenied(org.springframework.security.access.AccessDeniedException ex,
                                                           HttpServletRequest request) {
        if (securityEvents != null) {
            securityEvents.accessDenied(request, "METHOD");
        }
        return respond(errorResponse.build(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
                "Access denied.", request, ex, null));
    }

    @ExceptionHandler(BadCredentialException.class)
    public ResponseEntity<Object> handleBadCredential(BadCredentialException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "Invalid credentials provided.", request, ex, null));
    }

    @ExceptionHandler(com.sni.bokaticowork.core.exception.customs.BackOfficeAccessDeniedException.class)
    public ResponseEntity<Object> handleBackOfficeAccessDenied(com.sni.bokaticowork.core.exception.customs.BackOfficeAccessDeniedException ex,
                                                               HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BACK_OFFICE_ACCESS_DENIED, HttpStatus.FORBIDDEN,
                "Ce compte n'a pas accès à l'espace d'administration.", request, ex, null));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Object> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.ACCOUNT_LOCKED, HttpStatus.UNAUTHORIZED,
                ex.getMessage(), request, ex, null));
    }

    // ── Spring MVC / binding exceptions ──────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                "Invalid request parameters.", request, ex, errors));
    }

    /**
     * Une contrainte d'entite violee a l'enregistrement remonte enveloppee dans une exception de
     * transaction : c'est une donnee refusee (400), pas une panne du serveur (500).
     */
    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ResponseEntity<Object> handleTransactionSystem(org.springframework.transaction.TransactionSystemException ex,
                                                          HttpServletRequest request) {
        Throwable cause = ex;
        while (cause != null && !(cause instanceof jakarta.validation.ConstraintViolationException)) {
            cause = cause.getCause();
        }
        if (cause instanceof jakarta.validation.ConstraintViolationException violations) {
            List<String> errors = violations.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage())
                    .sorted()
                    .toList();
            return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Valeur refusée à l'enregistrement.", request, ex, errors));
        }
        // Lot KG6 : transaction annulee a la validation (contrainte, conflit) : 409, pas 500.
        return handleRolledBack(ex, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBind(BindException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                "Invalid request parameters.", request, ex, errors));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingRequestPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "Missing required request part: " + ex.getRequestPartName() + ".", request, ex, null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingRequestParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.MISSING_PARAMETER, HttpStatus.BAD_REQUEST,
                "Paramètre obligatoire manquant : " + ex.getParameterName() + ".", request, ex, null));
    }

    /** Lot KG6 : fichier ou requete trop lourds : 413 (avant : repli en 500). */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex,
                                                      HttpServletRequest request) {
        long max = ex.getMaxUploadSize();
        String limit = max > 0 ? " (maximum " + (max >= 1024 * 1024 ? (max / (1024 * 1024)) + " Mo" : (max / 1024) + " Ko") + ")" : "";
        return respond(errorResponse.build(ErrorCode.FILE_SIZE_EXCEEDED, HttpStatus.PAYLOAD_TOO_LARGE,
                "Fichier ou envoi trop volumineux" + limit + ".", request, ex, null));
    }

    /**
     * Lot KG6 : valeur de parametre ou de chemin impossible a convertir (nombre, date, enumeration) :
     * 400 qui nomme le parametre et, pour une enumeration, les valeurs admises (avant : 500).
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
                                                     HttpServletRequest request) {
        Class<?> type = ex.getRequiredType();
        String allowed = allowedValues(type);
        String message = "Valeur « " + ex.getValue() + " » invalide pour le paramètre " + ex.getName()
                + (allowed == null ? (type == null ? "" : " (type attendu : " + type.getSimpleName() + ")") : " · valeurs admises : " + allowed) + ".";
        return respond(errorResponse.build(ErrorCode.TYPE_MISMATCH, HttpStatus.BAD_REQUEST, message, request, ex,
                List.of(ex.getName() + " : " + (allowed == null ? "valeur invalide" : "attendu parmi " + allowed))));
    }

    /**
     * Lot KG6 : une transaction annulee en cours de route (verrou optimiste, annulation forcee) est
     * un conflit a reessayer : 409 au lieu de 500.
     */
    @ExceptionHandler({org.springframework.transaction.UnexpectedRollbackException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<Object> handleRolledBack(Exception ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                "L'opération n'a pas pu aboutir car les données ont changé entre-temps ou une étape a échoué. Rechargez puis réessayez.",
                request, ex, null));
    }

    private static String allowedValues(Class<?> type) {
        if (type == null || !type.isEnum()) {
            return null;
        }
        Object[] constants = type.getEnumConstants();
        StringBuilder values = new StringBuilder();
        for (Object constant : constants) {
            if (values.length() > 0) values.append(", ");
            values.append(((Enum<?>) constant).name());
        }
        return values.toString();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Une propriete inconnue merite d'etre nommee. Repondre "malformed" sans dire laquelle
        // laisse chercher, alors que la cause est presque toujours une clef mal orthographiee :
        // c'est exactement le defaut qui a laisse catalogSourceCode desarmer silencieusement les
        // garde-fous de remise pendant des semaines.
        // La cause n'est pas toujours directe : quand la propriete fautive est dans une liste,
        // Jackson enveloppe l'erreur d'un niveau. On parcourt donc la chaine.
        UnrecognizedPropertyException unknown = unrecognizedProperty(ex);
        if (unknown != null) {
            String known = unknown.getKnownPropertyIds() == null
                    ? ""
                    : unknown.getKnownPropertyIds().stream()
                            .map(String::valueOf)
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(", "));
            List<String> errors = List.of(
                    unknown.getPropertyName() + " : propriete inconnue"
                            + (known.isBlank() ? "" : " · attendu parmi : " + known));
            return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Propriete inconnue dans le corps de la requete : " + unknown.getPropertyName() + ".",
                    request, ex, errors));
        }
        // Lot KG6 : une valeur d'enumeration ou un format invalide dans le corps est nomme, avec les
        // valeurs admises.
        tools.jackson.databind.exc.InvalidFormatException invalid = invalidFormat(ex);
        if (invalid != null) {
            String field = invalid.getPath() == null || invalid.getPath().isEmpty() ? "?"
                    : invalid.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                    .collect(java.util.stream.Collectors.joining("."));
            String allowed = allowedValues(invalid.getTargetType());
            return respond(errorResponse.build(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Valeur « " + invalid.getValue() + " » invalide pour " + field
                            + (allowed == null ? "." : " · valeurs admises : " + allowed + "."),
                    request, ex, List.of(field + " : " + (allowed == null ? "format invalide" : "attendu parmi " + allowed))));
        }
        return respond(errorResponse.build(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                "Corps de la requête absent ou illisible.", request, ex, null));
    }

    private tools.jackson.databind.exc.InvalidFormatException invalidFormat(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof tools.jackson.databind.exc.InvalidFormatException invalid) {
                return invalid;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }

    private UnrecognizedPropertyException unrecognizedProperty(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof UnrecognizedPropertyException unknown) {
                return unknown;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        String supported = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");
        return respond(errorResponse.build(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type '" + ex.getContentType() + "' is not supported. Supported: " + supported + ".",
                request, ex, null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.METHOD_NOT_SUPPORTED, HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint.", request, ex, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.ENDPOINT_NOT_FOUND, HttpStatus.NOT_FOUND,
                "The requested endpoint does not exist.", request, ex, null));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest request) {
        return respond(errorResponse.build(ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again or contact support.", request, ex, null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<Object> respond(ApiError apiError) {
        return new ResponseEntity<>(apiError, HttpStatus.valueOf(apiError.getStatus()));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
