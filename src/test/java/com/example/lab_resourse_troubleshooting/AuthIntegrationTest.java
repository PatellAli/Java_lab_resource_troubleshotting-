package com.example.lab_resourse_troubleshooting;

import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testPasswordHashing() {
        String rawPassword = "SecretPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("WrongPassword", encodedPassword));
    }

    @Test
    void testPublicRegistrationForcesStudentRole() throws Exception {
        String registerJson = "{\"name\":\"Test Student\",\"email\":\"student@lab.com\",\"password\":\"Password123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Student"))
                .andExpect(jsonPath("$.email").value("student@lab.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User registeredUser = userRepository.findByEmail("student@lab.com").orElse(null);
        assertNotNull(registeredUser);
        assertEquals(Role.STUDENT, registeredUser.getRole());
        assertTrue(passwordEncoder.matches("Password123", registeredUser.getPasswordHash()));
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {
        User user = User.builder()
                .name("Valid User")
                .email("valid@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build();
        userRepository.save(user);

        String loginJson = "{\"email\":\"valid@lab.com\",\"password\":\"Password123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("valid@lab.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void testLoginWithInvalidPassword() throws Exception {
        User user = User.builder()
                .name("Valid User")
                .email("valid@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build();
        userRepository.save(user);

        String loginJson = "{\"email\":\"valid@lab.com\",\"password\":\"WrongPassword\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRoleBasedAccessControlRestrictions() throws Exception {
        // Unauthenticated access to restricted endpoint should fail
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@lab.com", roles = {"STUDENT"})
    void testStudentDeniedAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminAccessAllowedToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isOk());
    }
}
