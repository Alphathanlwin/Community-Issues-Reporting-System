package com.uit.scirs.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private String message;
    private LocalDateTime timestamp;
    private int status;
    private Map<String, String> errors;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, LocalDateTime timestamp, int status) {
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    public ErrorResponse(String message, LocalDateTime timestamp, int status, Map<String, String> errors) {
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}
