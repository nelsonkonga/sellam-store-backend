package com.sellam.store.common.exception;

import com.sellam.store.payments.exceptions.PaymentProviderUnavailableException;
import com.sellam.store.subscriptions.security.SubscriptionExpiredException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex)
    {
        logger.warn("Illegal argument error: {}", ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Invalid request")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex)
    {
        logger.warn("Validation error: {}", ex.getMessage());
        
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

    @ExceptionHandler(SubscriptionExpiredException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionExpired(SubscriptionExpiredException ex)
    {
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("SUBSCRIPTION_EXPIRED")
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex)
    {
        logger.warn("Response status exception: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getReason())
                .error(ex.getStatusCode().toString())
                .status(ex.getStatusCode().value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex)
    {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Not found")
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PaymentProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProviderUnavailable(PaymentProviderUnavailableException ex)
    {
        logger.warn("Fournisseur de paiement indisponible : {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("PAYMENT_PROVIDER_UNAVAILABLE")
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex)
    {
        logger.error("Unhandled exception", ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .message("Une erreur interne est survenue. Réessayez dans un instant.")
                .error("INTERNAL_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}