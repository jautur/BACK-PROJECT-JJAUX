package com.avisa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor d'excepcions global per capturar errors de Bean Validation (@Valid)
 * i retornar missatges d'error comprensibles amb format JSON i codi HTTP 400.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de validació de dades");
        body.put("missatges", errors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", ex.getMessage());
        body.put("missatges", Map.of("email", ex.getMessage()));

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(com.avisa.service.MassaIntentsException.class)
    public ResponseEntity<Map<String, Object>> handleMassaIntents(com.avisa.service.MassaIntentsException ex) {
        ResponseEntity<Map<String, Object>> resposta = error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        return ResponseEntity.status(resposta.getStatusCode()).header("Retry-After", String.valueOf(ex.minuts() * 60)).body(resposta.getBody());
    }

    @ExceptionHandler(com.avisa.imatges.ImatgeNoValidaException.class)
    public ResponseEntity<Map<String, Object>> handleImatgeNoValida(com.avisa.imatges.ImatgeNoValidaException ex) {
        return error(ex.massaGran() ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** El fitxer passa del límit del multipart (el mateix AVISA_IMATGES_MAX_BYTES) abans d'arribar al controlador. */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "El fitxer és massa gran.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Un error de la base no es pot fer passar per un resultat correcte, ni exposar el seu SQL. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        log.error("Error d'accés a dades", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "No s'ha pogut completar l'operació.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
