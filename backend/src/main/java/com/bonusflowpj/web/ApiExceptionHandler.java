package com.bonusflowpj.web;

import com.bonusflowpj.service.BusinessRuleException;
import com.bonusflowpj.service.FieldValidationException;
import com.bonusflowpj.web.dto.ErrorResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> businessRule(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Business rule violation",
            exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.computeIfAbsent(error.getField(), ignored -> new ArrayList<>()).add(error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation error",
            "Existem campos inválidos no formulário.",
            fieldErrors
        ));
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ErrorResponse> fieldValidation(FieldValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation error",
            exception.getMessage(),
            exception.getFieldErrors()
        ));
    }
}
