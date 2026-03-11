package io.github.balasis.taskmanager.context.web.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.critical.CriticalException;
import io.github.balasis.taskmanager.context.base.exception.auth.AuthenticationIntegrityException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.DownloadBudgetExceededException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RateLimitExceededException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.RepeatDownloadBlockedException;
import io.github.balasis.taskmanager.context.base.exception.ratelimit.ServiceOverloadedException;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import io.github.balasis.taskmanager.context.base.exception.blob.download.BlobDownloadTaskFileException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskFileBlobNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CriticalException.class)
    public ResponseEntity<String> handleCriticalException(CriticalException e) {
        logger.error("Critical infrastructure failure: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("A service error occurred, please try again later");
    }

    @ExceptionHandler(TaskManagerException.class)
    public ResponseEntity<String> handleTaskManagerException(TaskManagerException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<String> handleBusinessRuleException(BusinessRuleException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Required file part is missing: " + e.getRequestPartName());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid multipart request: " + e.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<String> handleUnauthenticated(UnauthenticatedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEnum(HttpMessageNotReadableException ex) {
        String message = ex.getCause() instanceof InvalidFormatException
                ? "Invalid value for field: " + ((InvalidFormatException) ex.getCause()).getValue()
                : "Malformed request";

        Map<String, String> error = Map.of("error", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AuthenticationIntegrityException.class)
    public ResponseEntity<String> handleAuthenticationIntegrityException(AuthenticationIntegrityException e){
        logger.warn("Authentication integrity failure: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Authentication error, please try again or contact support");
    }

    @ExceptionHandler(BlobUploadException.class)
    public ResponseEntity<String> handleBlobUpload(BlobUploadException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ex.getMessage());
    }

    @ExceptionHandler(BlobDownloadTaskFileException.class)
    public ResponseEntity<String> handleTaskFileDownload(BlobDownloadTaskFileException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ex.getMessage());
    }

    @ExceptionHandler(TaskFileBlobNotFoundException.class)
    public ResponseEntity<String> handleMissingBlob(TaskFileBlobNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(ex.getMessage());
    }

    @ExceptionHandler(DownloadBudgetExceededException.class)
    public ResponseEntity<String> handleDownloadBudgetExceeded(DownloadBudgetExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ex.getMessage());
    }

    @ExceptionHandler(RepeatDownloadBlockedException.class)
    public ResponseEntity<String> handleRepeatDownloadBlocked(RepeatDownloadBlockedException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<String> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(ex.getMessage());
    }

    @ExceptionHandler(ServiceOverloadedException.class)
    public ResponseEntity<String> handleServiceOverloaded(ServiceOverloadedException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ex.getMessage());
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<String> handleDownloadPoolFull(TaskRejectedException ex) {
        logger.warn("Download pool saturated, rejecting async request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Download will take a bit to begin due to server receiving unexpected traffic");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body("Unsupported content type: " + ex.getContentType());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred, please try again later");
    }
}
