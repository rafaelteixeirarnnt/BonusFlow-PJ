package com.bonusflowpj.service;

import java.util.List;
import java.util.Map;

public class FieldValidationException extends RuntimeException {

    private final Map<String, List<String>> fieldErrors;

    public FieldValidationException(Map<String, List<String>> fieldErrors) {
        super("Existem campos inválidos no formulário.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
