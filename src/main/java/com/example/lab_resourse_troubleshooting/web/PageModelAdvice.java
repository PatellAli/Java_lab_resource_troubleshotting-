package com.example.lab_resourse_troubleshooting.web;

import com.example.lab_resourse_troubleshooting.dto.UserResponseDto;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects the authenticated user and their role into the model of every
 * server-rendered page so templates can render role-aware navigation.
 * Authorization itself is still enforced by Spring Security and the services.
 */
@ControllerAdvice(basePackages = "com.example.lab_resourse_troubleshooting.web")
public class PageModelAdvice {

    private final UserRepository userRepository;

    public PageModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public UserResponseDto currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
                .map(UserResponseDto::fromEntity)
                .orElse(null);
    }

    @ModelAttribute("currentRole")
    public String currentRole(Authentication authentication) {
        UserResponseDto user = currentUser(authentication);
        return user != null ? user.getRole().name() : null;
    }
}
