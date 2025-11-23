package com.cliq24.backend.service;

import com.cliq24.backend.dto.LoginResponseDTO;
import com.cliq24.backend.dto.UserDTO;
import com.cliq24.backend.mapper.UserMapper;
import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import com.cliq24.backend.util.JwtUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    
    private static final Logger logger = LogManager.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public AuthService(UserRepository userRepository, UserMapper userMapper, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }
    
    public LoginResponseDTO handleGoogleCallback(String code) {
        logger.info("Handling Google OAuth callback");
        
        try {
            User user = createOrUpdateUser(
                "google_123456",
                "user@example.com",
                "Demo User",
                "https://via.placeholder.com/100"
            );
            
            String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail());
            UserDTO userDTO = userMapper.toDTOWithToken(user, jwtToken);
            
            LoginResponseDTO response = new LoginResponseDTO();
            response.setUser(userDTO);
            response.setToken(jwtToken);
            response.setExpiresIn(86400L);
            
            logger.info("User {} logged in successfully", user.getEmail());
            return response;
            
        } catch (Exception e) {
            logger.error("Error during Google login: {}", e.getMessage(), e);
            throw new RuntimeException("Login failed", e);
        }
    }
    
    public UserDTO getUserFromToken(String authHeader) {
        logger.debug("Extracting user from token");
        
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null) {
            logger.warn("Invalid token provided");
            throw new RuntimeException("Invalid token");
        }
        
        String userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId);
                    return new RuntimeException("User not found");
                });
        
        return userMapper.toDTO(user);
    }
    
    private User createOrUpdateUser(String googleId, String email, String name, String picture) {
        logger.debug("Creating or updating user: {}", email);
        
        return userRepository.findByGoogleId(googleId)
                .map(existingUser -> {
                    logger.info("Updating existing user: {}", email);
                    existingUser.setEmail(email);
                    existingUser.setName(name);
                    existingUser.setPicture(picture);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    logger.info("Creating new user: {}", email);
                    User newUser = new User();
                    newUser.setGoogleId(googleId);
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setPicture(picture);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(newUser);
                });
    }
    
    public String validateAndExtractUserId(String authHeader) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null || jwtUtil.isTokenExpired(token)) {
            logger.warn("Invalid or expired token");
            throw new RuntimeException("Invalid or expired token");
        }
        return jwtUtil.extractUserId(token);
    }

    public UserDTO updateUserPicture(String authHeader, String pictureUrl) {
        logger.debug("Updating user profile picture");

        String userId = validateAndExtractUserId(authHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId);
                    return new RuntimeException("User not found");
                });

        user.setPicture(pictureUrl);
        User updatedUser = userRepository.save(user);

        logger.info("Profile picture updated for user: {}", user.getEmail());
        return userMapper.toDTO(updatedUser);
    }
}
