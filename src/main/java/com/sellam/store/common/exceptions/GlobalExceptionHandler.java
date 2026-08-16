package com.sellam.store.common.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String message;
        private String error;
        private int status;
        private LocalDateTime timestamp;
        private Map<String, String> validationErrors;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument error: {}", ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Invalid request")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Access-Control-Allow-Origin", "*")
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse response = ErrorResponse.builder()
                .message("Validation failed")
                .error("Invalid request body")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Response status exception: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getReason())
                .error(ex.getStatusCode().toString())
                .status(ex.getStatusCode().value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .message("Une erreur interne est survenue")
                .error(ex.getClass().getSimpleName())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
