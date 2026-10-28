package com.example.lab_resourse_troubleshooting.dto;

import com.example.lab_resourse_troubleshooting.entity.Lab;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabSummaryDto {
    private Long id;
    private String name;
    private String location;

    public LabSummaryDto() {
    }

    public LabSummaryDto(Long id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public LabSummaryDto(Lab lab) {
        if (lab != null) {
            this.id = lab.getId();
            this.name = lab.getName();
            this.location = lab.getLocation();
        }
    }

    public static LabSummaryDto fromEntity(Lab lab) {
        if (lab == null) {
            return null;
        }
        return new LabSummaryDto(lab);
    }
}
