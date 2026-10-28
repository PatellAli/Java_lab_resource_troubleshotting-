package com.example.lab_resourse_troubleshooting.controller;

import com.example.lab_resourse_troubleshooting.dto.AssignTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.LabRequestDto;
import com.example.lab_resourse_troubleshooting.dto.LabResponseDto;
import com.example.lab_resourse_troubleshooting.service.LabService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/labs")
public class AdminLabController {

    private final LabService labService;

    public AdminLabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping
    public ResponseEntity<List<LabResponseDto>> getAllLabs() {
        return ResponseEntity.ok(labService.getAllLabs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponseDto> getLabById(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getLabById(id));
    }

    @PostMapping
    public ResponseEntity<LabResponseDto> createLab(@Valid @RequestBody LabRequestDto request) {
        LabResponseDto createdLab = labService.createLab(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLab);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabResponseDto> updateLab(
            @PathVariable Long id,
            @Valid @RequestBody LabRequestDto request) {
        LabResponseDto updatedLab = labService.updateLab(id, request);
        return ResponseEntity.ok(updatedLab);
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<LabResponseDto> assignTechnician(
            @PathVariable Long id,
            @RequestBody AssignTechnicianRequestDto request) {
        LabResponseDto updatedLab = labService.assignTechnician(id, request.getTechnicianId());
        return ResponseEntity.ok(updatedLab);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LabResponseDto> updateLabStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        LabResponseDto updatedLab = labService.updateLabStatus(id, active);
        return ResponseEntity.ok(updatedLab);
    }
}
