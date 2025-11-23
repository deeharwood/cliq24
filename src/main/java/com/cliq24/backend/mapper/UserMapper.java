package com.cliq24.backend.mapper;

import com.cliq24.backend.dto.UserDTO;
import com.cliq24.backend.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(user.getId())
                .googleId(user.getGoogleId())
                .email(user.getEmail())
                .name(user.getName())
                .picture(user.getPicture())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    public UserDTO toDTOWithToken(User user, String token) {
        UserDTO dto = toDTO(user);
        if (dto != null) {
            dto.setToken(token);
        }
        return dto;
    }
    
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(dto.getId());
        user.setGoogleId(dto.getGoogleId());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setPicture(dto.getPicture());
        user.setCreatedAt(dto.getCreatedAt());
        
        return user;
    }
}
