package com.example.lab_resourse_troubleshooting.config;

import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${INITIAL_ADMIN_NAME:System Administrator}")
    private String adminName;

    @Value("${INITIAL_ADMIN_EMAIL:admin@lab.com}")
    private String adminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:Admin@12345}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<User> existingAdmins = userRepository.findByRole(Role.ADMIN);
        if (existingAdmins.isEmpty()) {
            String email = adminEmail.trim().toLowerCase();
            if (!userRepository.existsByEmail(email)) {
                User admin = new User();
                admin.setName(adminName.trim());
                admin.setEmail(email);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);

                userRepository.save(admin);
                log.info("Default Admin account created: {}", email);
            }
        }
    }
}
