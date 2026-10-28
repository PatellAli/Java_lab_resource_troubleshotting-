package com.example.lab_resourse_troubleshooting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LabRequestDto {

    @NotBlank(message = "Lab name is required")
    private String name;

    private String location;

    private Boolean active = true;

    private Long assignedTechnicianId;
}
