package com.example.order.error;

import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse (
        Instant timestamp,
        int status,
        String error,
        String path,
        List<FieldValidationError> fieldErrors
){
}
