package com.example.lab_resourse_troubleshooting.web;

import com.example.lab_resourse_troubleshooting.dto.LoginRequest;
import com.example.lab_resourse_troubleshooting.dto.RegisterRequest;
import com.example.lab_resourse_troubleshooting.dto.UserResponseDto;
import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthPageController {

    private final AuthService authService;

    public AuthPageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }
        return "redirect:" + dashboardPath(authentication);
    }

    @GetMapping("/login")
    public String loginForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:" + dashboardPath(authentication);
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginRequest());
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginRequest loginForm,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        try {
            UserResponseDto user = authService.login(loginForm, request, response);
            return "redirect:" + dashboardPath(user.getRole());
        } catch (AuthenticationException ex) {
            model.addAttribute("loginError", "Invalid email or password. Please try again.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:" + dashboardPath(authentication);
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterRequest registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            authService.registerStudent(registerForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully. Please sign in with your new credentials.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registerError", ex.getMessage());
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String dashboardPath(Authentication authentication) {
        var authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        if (authorities.contains("ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        if (authorities.contains("ROLE_TECHNICIAN")) {
            return "/technician/complaints";
        }
        return "/student/complaints";
    }

    private String dashboardPath(Role role) {
        return switch (role) {
            case ADMIN -> "/admin/dashboard";
            case TECHNICIAN -> "/technician/complaints";
            case STUDENT -> "/student/complaints";
        };
    }
}
