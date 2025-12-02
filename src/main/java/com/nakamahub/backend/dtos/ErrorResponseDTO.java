package com.nakamahub.backend.dtos;

import lombok.Data;

@Data
public class ErrorResponseDTO {
    private int status;
    private String message;
    private String path;
    private String timestamp;

    public ErrorResponseDTO(int status, String message, String path, String timestamp) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }
}
