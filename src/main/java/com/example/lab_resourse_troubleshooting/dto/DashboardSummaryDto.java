package com.example.lab_resourse_troubleshooting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private long totalComplaints;
    private long pendingComplaints;
    private long inProgressComplaints;
    private long completedComplaints;
    private long labsWithoutTechnicianCount;

    private List<ChartDataDto> complaintsByLab;
    private List<ChartDataDto> complaintsByType;
    private List<ChartDataDto> complaintsByStatus;

    private List<LabSummaryDto> labsWithoutTechnician;
}
