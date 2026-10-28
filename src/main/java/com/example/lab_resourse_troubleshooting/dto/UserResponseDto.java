package com.example.lab_resourse_troubleshooting.dto;

import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;

    public UserResponseDto() {
    }

    public UserResponseDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.active = user.isActive();
            this.createdAt = user.getCreatedAt();
        }
    }

    public static UserResponseDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDto(user);
    }
}
