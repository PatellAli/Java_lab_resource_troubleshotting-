package com.example.lab_resourse_troubleshooting.controller;

import com.example.lab_resourse_troubleshooting.dto.CreateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.UserResponseDto;
import com.example.lab_resourse_troubleshooting.service.TechnicianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/technicians")
public class AdminTechnicianController {

    private final TechnicianService technicianService;

    public AdminTechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllTechnicians() {
        return ResponseEntity.ok(technicianService.getAllTechnicians());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getTechnicianById(@PathVariable Long id) {
        return ResponseEntity.ok(technicianService.getTechnicianById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createTechnician(@Valid @RequestBody CreateTechnicianRequestDto request) {
        UserResponseDto createdTechnician = technicianService.createTechnician(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTechnician);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateTechnician(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTechnicianRequestDto request) {
        UserResponseDto updatedTechnician = technicianService.updateTechnician(id, request);
        return ResponseEntity.ok(updatedTechnician);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> updateTechnicianStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        UserResponseDto updatedTechnician = technicianService.updateTechnicianStatus(id, active);
        return ResponseEntity.ok(updatedTechnician);
    }
}
