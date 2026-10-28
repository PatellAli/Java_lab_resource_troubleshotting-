package com.example.lab_resourse_troubleshooting.service;

import com.example.lab_resourse_troubleshooting.dto.CreateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.UserResponseDto;
import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TechnicianService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TechnicianService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllTechnicians() {
        List<UserResponseDto> list = new ArrayList<>();
        for (User user : userRepository.findByRole(Role.TECHNICIAN)) {
            list.add(new UserResponseDto(user));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getTechnicianById(Long id) {
        User user = userRepository.findByIdAndRole(id, Role.TECHNICIAN)
                .orElseThrow(() -> new NoSuchElementException("Technician not found with id: " + id));
        return new UserResponseDto(user);
    }

    @Transactional
    public UserResponseDto createTechnician(CreateTechnicianRequestDto request) {
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email is already registered: " + trimmedEmail);
        }

        User technician = new User();
        technician.setName(request.getName().trim());
        technician.setEmail(trimmedEmail);
        technician.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        technician.setRole(Role.TECHNICIAN);
        technician.setActive(true);

        User savedUser = userRepository.save(technician);
        return new UserResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateTechnician(Long id, UpdateTechnicianRequestDto request) {
        User user = userRepository.findByIdAndRole(id, Role.TECHNICIAN)
                .orElseThrow(() -> new NoSuchElementException("Technician not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User savedUser = userRepository.save(user);
        return new UserResponseDto(savedUser);
    }

    @Transactional
    public UserResponseDto updateTechnicianStatus(Long id, boolean active) {
        User user = userRepository.findByIdAndRole(id, Role.TECHNICIAN)
                .orElseThrow(() -> new NoSuchElementException("Technician not found with id: " + id));

        user.setActive(active);
        User savedUser = userRepository.save(user);
        return new UserResponseDto(savedUser);
    }
}
