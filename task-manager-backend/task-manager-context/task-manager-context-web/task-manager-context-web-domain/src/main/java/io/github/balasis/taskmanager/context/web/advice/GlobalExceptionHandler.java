package io.github.balasis.taskmanager.context.web.advice;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import io.github.balasis.taskmanager.context.base.exception.auth.UnauthenticatedException;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

}
