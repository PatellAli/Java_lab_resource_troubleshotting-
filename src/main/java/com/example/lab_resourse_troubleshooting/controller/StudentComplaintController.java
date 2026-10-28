package com.example.lab_resourse_troubleshooting.controller;

import com.example.lab_resourse_troubleshooting.dto.ComplaintResponseDto;
import com.example.lab_resourse_troubleshooting.dto.CreateComplaintRequestDto;
import com.example.lab_resourse_troubleshooting.dto.LabResponseDto;
import com.example.lab_resourse_troubleshooting.dto.PageResponseDto;
import com.example.lab_resourse_troubleshooting.service.ComplaintService;
import com.example.lab_resourse_troubleshooting.service.LabService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentComplaintController {

    private final ComplaintService complaintService;
    private final LabService labService;

    public StudentComplaintController(ComplaintService complaintService, LabService labService) {
        this.complaintService = complaintService;
        this.labService = labService;
    }

    @GetMapping("/labs")
    public ResponseEntity<List<LabResponseDto>> getActiveLabsForComplaint() {
        return ResponseEntity.ok(labService.getActiveLabs());
    }

    @PostMapping("/complaints")
    public ResponseEntity<ComplaintResponseDto> createComplaint(
            @Valid @RequestBody CreateComplaintRequestDto request,
            Authentication authentication) {
        ComplaintResponseDto response = complaintService.createComplaint(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/complaints")
    public ResponseEntity<PageResponseDto<ComplaintResponseDto>> getMyComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(complaintService.getStudentComplaints(authentication.getName(), pageable));
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> getMyComplaintById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.getStudentComplaintById(authentication.getName(), id));
    }
}
