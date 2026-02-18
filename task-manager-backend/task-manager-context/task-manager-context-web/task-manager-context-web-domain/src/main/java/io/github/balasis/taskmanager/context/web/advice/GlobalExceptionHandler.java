package io.github.balasis.taskmanager.context.web.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.auth.AuthenticationIntegrityException;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import io.github.balasis.taskmanager.context.base.exception.blob.download.BlobDownloadTaskFileException;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadException;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskFileBlobNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
    public ResponseEntity<String> AuthenticationIntegrityException(AuthenticationIntegrityException e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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
}
