package com.example.lab_resourse_troubleshooting.dto;

import com.example.lab_resourse_troubleshooting.entity.Complaint;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.entity.ComplaintType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ComplaintResponseDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long labId;
    private String labName;
    private Long technicianId;
    private String technicianName;
    private String technicianEmail;
    private String resourceNumber;
    private ComplaintType complaintType;
    private String description;
    private ComplaintStatus status;
    private String technicianNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ComplaintResponseDto() {
    }

    public ComplaintResponseDto(Complaint complaint) {
        if (complaint != null) {
            this.id = complaint.getId();
            if (complaint.getStudent() != null) {
                this.studentId = complaint.getStudent().getId();
                this.studentName = complaint.getStudent().getName();
                this.studentEmail = complaint.getStudent().getEmail();
            }
            if (complaint.getLab() != null) {
                this.labId = complaint.getLab().getId();
                this.labName = complaint.getLab().getName();
            }
            if (complaint.getTechnician() != null) {
                this.technicianId = complaint.getTechnician().getId();
                this.technicianName = complaint.getTechnician().getName();
                this.technicianEmail = complaint.getTechnician().getEmail();
            }
            this.resourceNumber = complaint.getResourceNumber();
            this.complaintType = complaint.getComplaintType();
            this.description = complaint.getDescription();
            this.status = complaint.getStatus();
            this.technicianNote = complaint.getTechnicianNote();
            this.createdAt = complaint.getCreatedAt();
            this.updatedAt = complaint.getUpdatedAt();
        }
    }

    public static ComplaintResponseDto fromEntity(Complaint complaint) {
        if (complaint == null) {
            return null;
        }
        return new ComplaintResponseDto(complaint);
    }
}
