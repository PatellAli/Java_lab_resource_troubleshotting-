package com.example.lab_resourse_troubleshooting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTechnicianRequestDto {
    private String name;
    private Boolean active;
}
