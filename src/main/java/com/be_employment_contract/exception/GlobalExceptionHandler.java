package com.be_employment_contract.exception;

import com.be_employment_contract.constant.ApiCode;
import com.be_employment_contract.response.ApiResponse;
import com.be_employment_contract.response.ErrorField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        if (containsFileCountLimitCause(exception)) {
            log.warn("Upload rejected: max file count exceeded", exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ApiCode.UPLOAD_FILE_COUNT_EXCEEDED,
                    "Too many attachments in one request. Please reduce the number of files"));
        }

        log.warn("Upload rejected: max upload size exceeded", exception);
        return ResponseEntity.status(413)
            .body(ApiResponse.error(ApiCode.UPLOAD_SIZE_EXCEEDED,
                "Upload too large. Please reduce file size or number of attachments"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        log.warn("Unsupported content type", exception);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(ApiResponse.error(ApiCode.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Content-Type. Use application/json or multipart/form-data"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestPart(MissingServletRequestPartException exception) {
        log.warn("Missing multipart request part: {}", exception.getRequestPartName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ApiCode.MISSING_REQUEST_PART,
                "Missing required multipart field: " + exception.getRequestPartName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("Request body parse error", exception);

        String message = "Invalid request body format";
        Throwable cause = exception.getMostSpecificCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.contains("LocalDate")) {
                message = "Invalid date format. Please use yyyy-MM-dd (example: 2026-03-24)";
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ApiCode.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        if (containsFileCountLimitCause(exception)) {
            log.warn("Upload rejected: max file count exceeded", exception);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ApiCode.UPLOAD_FILE_COUNT_EXCEEDED,
                    "Too many attachments in one request. Please reduce the number of files"));
        }

        log.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ApiCode.INTERNAL_ERROR, "Unexpected server error"));
    }

    private ErrorField toErrorField(FieldError fieldError) {
        return new ErrorField(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private boolean containsFileCountLimitCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            if ("org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException".equals(className)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

