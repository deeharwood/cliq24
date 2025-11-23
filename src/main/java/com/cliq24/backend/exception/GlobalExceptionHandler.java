package com.cliq24.backend.exception;

import com.cliq24.backend.dto.ErrorResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        logger.error("Runtime exception: ", ex);
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .message(ex.getMessage())
                .error("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        logger.error("Illegal argument: ", ex);
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .message(ex.getMessage())
                .error("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false))
                .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
