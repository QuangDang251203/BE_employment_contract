package com.be_employment_contract.exception;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.response.ApiResponse;
import com.be_employment_contract.response.ErrorField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        log.warn("Business error: code={}, message={}", exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus())
            .body(ApiResponse.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ErrorField>>> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorField> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toErrorField)
            .toList();

        ApiResponse<List<ErrorField>> response = new ApiResponse<>(
            ApiCode.VALIDATION_ERROR,
            "Request validation failed",
            false,
            errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ApiCode.INTERNAL_ERROR, "Unexpected server error"));
    }

    private ErrorField toErrorField(FieldError fieldError) {
        return new ErrorField(fieldError.getField(), fieldError.getDefaultMessage());
    }
}

