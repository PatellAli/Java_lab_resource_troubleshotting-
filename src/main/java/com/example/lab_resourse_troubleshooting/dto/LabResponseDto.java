package com.example.lab_resourse_troubleshooting.dto;

import com.example.lab_resourse_troubleshooting.entity.Lab;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabResponseDto {

    private Long id;
    private String name;
    private String location;
    private boolean active;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private String assignedTechnicianEmail;

    public LabResponseDto() {
    }

    public LabResponseDto(Lab lab) {
        if (lab != null) {
            this.id = lab.getId();
            this.name = lab.getName();
            this.location = lab.getLocation();
            this.active = lab.isActive();
            if (lab.getAssignedTechnician() != null) {
                this.assignedTechnicianId = lab.getAssignedTechnician().getId();
                this.assignedTechnicianName = lab.getAssignedTechnician().getName();
                this.assignedTechnicianEmail = lab.getAssignedTechnician().getEmail();
            }
        }
    }

    public static LabResponseDto fromEntity(Lab lab) {
        if (lab == null) {
            return null;
        }
        return new LabResponseDto(lab);
    }
}
