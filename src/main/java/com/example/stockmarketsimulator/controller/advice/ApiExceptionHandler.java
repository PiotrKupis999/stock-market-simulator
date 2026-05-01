package com.example.stockmarketsimulator.controller.advice;

import com.example.stockmarketsimulator.exception.BadRequestException;
import com.example.stockmarketsimulator.exception.NotFoundException;
import com.example.stockmarketsimulator.exception.StorageOperationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            BadRequestException.class
    })
    ResponseEntity<Void> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({
            RedisConnectionFailureException.class,
            RedisSystemException.class,
            StorageOperationException.class
    })
    ResponseEntity<Void> serviceUnavailable() {
        return ResponseEntity.status(503).build();
    }
}
