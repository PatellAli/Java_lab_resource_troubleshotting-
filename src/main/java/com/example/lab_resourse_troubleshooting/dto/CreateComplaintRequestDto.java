package com.example.lab_resourse_troubleshooting.dto;

import com.example.lab_resourse_troubleshooting.entity.ComplaintType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateComplaintRequestDto {

    @NotNull(message = "Lab ID is required")
    private Long labId;

    @NotBlank(message = "Resource number is required")
    private String resourceNumber;

    @NotNull(message = "Complaint type is required")
    private ComplaintType complaintType;

    @NotBlank(message = "Description is required")
    private String description;
}
