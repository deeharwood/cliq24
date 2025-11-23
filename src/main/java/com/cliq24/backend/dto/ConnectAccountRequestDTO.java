package com.cliq24.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ConnectAccountRequestDTO {
    @NotBlank(message = "Platform is required")
    private String platform;

    @NotBlank(message = "Authorization code is required")
    private String code;

    private String state; // For CSRF protection

    public ConnectAccountRequestDTO() {
    }

    public ConnectAccountRequestDTO(String platform, String code, String state) {
        this.platform = platform;
        this.code = code;
        this.state = state;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
