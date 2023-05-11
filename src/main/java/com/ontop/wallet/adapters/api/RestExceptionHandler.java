package com.ontop.wallet.adapters.api;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.ontop.wallet.adapters.clients.WalletClientException;
import com.ontop.wallet.domain.exceptions.AccountNotFoundException;
import com.ontop.wallet.domain.exceptions.ResourceLockedException;
import com.ontop.wallet.domain.exceptions.TransactionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFoundException(final AccountNotFoundException exception) {
        final ApiError apiError = new ApiError("INVALID_USER", "no account found for user");
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceLockedException.class)
    public ResponseEntity<ApiError> handleResourceLockedException(final ResourceLockedException exception) {
        final ApiError apiError = new ApiError("RESOURCE_LOCKED", "user resource is locked by another process");
        return new ResponseEntity<>(apiError, HttpStatus.LOCKED);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ApiError> handleTransactionException(final TransactionException exception) {
        final ApiError apiError = new ApiError(exception.code(), exception.message());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WalletClientException.class)
    public ResponseEntity<ApiError> handleWalletClientException(final WalletClientException exception) {
        final ApiError apiError = new ApiError(exception.code(), exception.message());
        return new ResponseEntity<>(apiError, exception.status());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        final String errors = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        final ApiError apiError = new ApiError("INVALID_REQUEST", errors);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ApiError> handleJsonUnrecognizedPropertyException(final UnrecognizedPropertyException exception) {
        final ApiError apiError = new ApiError("INVALID_REQUEST", exception.getMessage());
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleResourceAccessException(final ResourceAccessException exception) {
        log.error("gateway timeout", exception);
        final ApiError apiError = new ApiError("GATEWAY_TIMEOUT", "gateway timeout");
        return new ResponseEntity<>(apiError, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(final Exception exception) {
        log.error("Request processing interrupted due to an unknown exception", exception);
        final ApiError apiError = new ApiError("SERVER_ERROR", "process was interrupted");
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
