package io.github.balasis.taskmanager.context.web.advice;

import io.github.balasis.taskmanager.context.base.exception.TaskManagerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskManagerException.class)
    public ResponseEntity<String> handleHotelException(TaskManagerException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

}
