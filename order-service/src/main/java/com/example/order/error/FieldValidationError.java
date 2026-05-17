package com.example.order.error;

public record FieldValidationError(
        String field,
        String message
) {
}
