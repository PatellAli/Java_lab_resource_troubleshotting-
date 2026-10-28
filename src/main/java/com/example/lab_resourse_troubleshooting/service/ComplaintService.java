package com.example.lab_resourse_troubleshooting.service;

import com.example.lab_resourse_troubleshooting.dto.ComplaintResponseDto;
import com.example.lab_resourse_troubleshooting.dto.CreateComplaintRequestDto;
import com.example.lab_resourse_troubleshooting.dto.PageResponseDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateComplaintStatusRequestDto;
import com.example.lab_resourse_troubleshooting.entity.Complaint;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.entity.Lab;
import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final EmailService emailService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            LabRepository labRepository,
                            EmailService emailService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.emailService = emailService;
    }

    @Transactional
    public ComplaintResponseDto createComplaint(String studentEmail, CreateComplaintRequestDto request) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with email: " + studentEmail));

        if (student.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only students can submit complaints");
        }

        if (!student.isActive()) {
            throw new IllegalArgumentException("Inactive students cannot submit complaints");
        }

        Lab lab = labRepository.findById(request.getLabId())
                .orElseThrow(() -> new IllegalArgumentException("Selected lab not found with id: " + request.getLabId()));

        if (!lab.isActive()) {
            throw new IllegalArgumentException("Complaints cannot be submitted for an inactive lab");
        }

        // Read lab's currently assigned technician
        User assignedTechnician = null;
        if (lab.getAssignedTechnician() != null && lab.getAssignedTechnician().isActive()) {
            assignedTechnician = lab.getAssignedTechnician();
        }

        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        complaint.setLab(lab);
        complaint.setTechnician(assignedTechnician);
        complaint.setResourceNumber(request.getResourceNumber().trim());
        complaint.setComplaintType(request.getComplaintType());
        complaint.setDescription(request.getDescription().trim());
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint savedComplaint = complaintRepository.save(complaint);
        return new ComplaintResponseDto(savedComplaint);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ComplaintResponseDto> getStudentComplaints(String studentEmail, Pageable pageable) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with email: " + studentEmail));

        Page<ComplaintResponseDto> page = complaintRepository.findByStudentId(student.getId(), pageable)
                .map(ComplaintResponseDto::new);

        return PageResponseDto.fromPage(page);
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDto getStudentComplaintById(String studentEmail, Long complaintId) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with email: " + studentEmail));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found with id: " + complaintId));

        if (!complaint.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("You are not authorized to view another student's complaint");
        }

        return new ComplaintResponseDto(complaint);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ComplaintResponseDto> getTechnicianComplaints(String technicianEmail, Pageable pageable) {
        User technician = userRepository.findByEmail(technicianEmail)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found with email: " + technicianEmail));

        Page<ComplaintResponseDto> page = complaintRepository.findByTechnicianId(technician.getId(), pageable)
                .map(ComplaintResponseDto::new);

        return PageResponseDto.fromPage(page);
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDto getTechnicianComplaintById(String technicianEmail, Long complaintId) {
        User technician = userRepository.findByEmail(technicianEmail)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found with email: " + technicianEmail));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found with id: " + complaintId));

        if (complaint.getTechnician() == null || !complaint.getTechnician().getId().equals(technician.getId())) {
            throw new AccessDeniedException("You are not authorized to view complaints assigned to another technician");
        }

        return new ComplaintResponseDto(complaint);
    }

    @Transactional
    public ComplaintResponseDto updateComplaintStatus(String technicianEmail, Long complaintId, UpdateComplaintStatusRequestDto request) {
        User technician = userRepository.findByEmail(technicianEmail)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found with email: " + technicianEmail));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found with id: " + complaintId));

        if (complaint.getTechnician() == null || !complaint.getTechnician().getId().equals(technician.getId())) {
            throw new AccessDeniedException("You are not authorized to update complaints assigned to another technician");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getStatus();

        // Prevent moving status back to PENDING
        if (newStatus == ComplaintStatus.PENDING) {
            throw new IllegalArgumentException("Complaint status cannot be moved back to PENDING");
        }

        // Prevent repeating the same status as a valid status transition
        if (previousStatus == newStatus) {
            throw new IllegalArgumentException("Complaint is already in status " + newStatus);
        }

        // Prevent modifying an already completed complaint
        if (previousStatus == ComplaintStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed complaints cannot be modified");
        }

        // Update status, note and timestamp
        complaint.setStatus(newStatus);
        if (request.getTechnicianNote() != null) {
            complaint.setTechnicianNote(request.getTechnicianNote().trim());
        }
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Send notification email to student
        emailService.sendStatusChangeEmail(savedComplaint, previousStatus, newStatus);

        return new ComplaintResponseDto(savedComplaint);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ComplaintResponseDto> getAllComplaintsForAdmin(Long labId, ComplaintStatus status, Pageable pageable) {
        Page<Complaint> complaints;

        if (labId != null && status != null) {
            complaints = complaintRepository.findByLabIdAndStatus(labId, status, pageable);
        } else if (labId != null) {
            complaints = complaintRepository.findByLabId(labId, pageable);
        } else if (status != null) {
            complaints = complaintRepository.findByStatus(status, pageable);
        } else {
            complaints = complaintRepository.findAll(pageable);
        }

        return PageResponseDto.fromPage(complaints.map(ComplaintResponseDto::new));
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDto getComplaintByIdForAdmin(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NoSuchElementException("Complaint not found with id: " + complaintId));

        return new ComplaintResponseDto(complaint);
    }
}
