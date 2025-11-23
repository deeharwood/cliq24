package com.cliq24.backend.dto;

import java.time.LocalDateTime;

public class ErrorResponseDTO {
    private String message;
    private String error;
    private Integer status;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponseDTO() {
    }

    public ErrorResponseDTO(String message, String error, Integer status,
                           LocalDateTime timestamp, String path) {
        this.message = message;
        this.error = error;
        this.status = status;
        this.timestamp = timestamp;
        this.path = path;
    }

    public static ErrorResponseDTOBuilder builder() {
        return new ErrorResponseDTOBuilder();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public static class ErrorResponseDTOBuilder {
        private String message;
        private String error;
        private Integer status;
        private LocalDateTime timestamp;
        private String path;

        ErrorResponseDTOBuilder() {
        }

        public ErrorResponseDTOBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseDTOBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseDTOBuilder status(Integer status) {
            this.status = status;
            return this;
        }

        public ErrorResponseDTOBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseDTOBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseDTO build() {
            return new ErrorResponseDTO(message, error, status, timestamp, path);
        }
    }
}
