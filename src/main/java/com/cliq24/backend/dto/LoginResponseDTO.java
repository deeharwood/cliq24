package com.cliq24.backend.dto;

public class LoginResponseDTO {
    private UserDTO user;
    private String token;
    private String refreshToken;
    private Long expiresIn; // in seconds

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(UserDTO user, String token, String refreshToken, Long expiresIn) {
        this.user = user;
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
