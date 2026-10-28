package com.example.lab_resourse_troubleshooting.controller;

import com.example.lab_resourse_troubleshooting.dto.ComplaintResponseDto;
import com.example.lab_resourse_troubleshooting.dto.PageResponseDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateComplaintStatusRequestDto;
import com.example.lab_resourse_troubleshooting.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technician/complaints")
public class TechnicianComplaintController {

    private final ComplaintService complaintService;

    public TechnicianComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public ResponseEntity<PageResponseDto<ComplaintResponseDto>> getAssignedComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(complaintService.getTechnicianComplaints(authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponseDto> getAssignedComplaintById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.getTechnicianComplaintById(authentication.getName(), id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintStatusRequestDto request,
            Authentication authentication) {
        ComplaintResponseDto response = complaintService.updateComplaintStatus(authentication.getName(), id, request);
        return ResponseEntity.ok(response);
    }
}
