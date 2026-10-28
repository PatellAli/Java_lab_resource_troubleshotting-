package com.example.lab_resourse_troubleshooting.service;

import com.example.lab_resourse_troubleshooting.dto.ChartDataDto;
import com.example.lab_resourse_troubleshooting.dto.DashboardSummaryDto;
import com.example.lab_resourse_troubleshooting.dto.LabSummaryDto;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.entity.Lab;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final ComplaintRepository complaintRepository;
    private final LabRepository labRepository;

    public DashboardService(ComplaintRepository complaintRepository, LabRepository labRepository) {
        this.complaintRepository = complaintRepository;
        this.labRepository = labRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        DashboardSummaryDto dto = new DashboardSummaryDto();

        // 1. Complaint counts by status
        long totalComplaints = complaintRepository.count();
        long pendingComplaints = complaintRepository.countByStatus(ComplaintStatus.PENDING);
        long inProgressComplaints = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long completedComplaints = complaintRepository.countByStatus(ComplaintStatus.COMPLETED);

        dto.setTotalComplaints(totalComplaints);
        dto.setPendingComplaints(pendingComplaints);
        dto.setInProgressComplaints(inProgressComplaints);
        dto.setCompletedComplaints(completedComplaints);

        // 2. Active labs without an active assigned technician
        List<Lab> unassignedLabs = labRepository.findActiveLabsWithoutActiveTechnician();
        dto.setLabsWithoutTechnicianCount(unassignedLabs.size());

        List<LabSummaryDto> unassignedList = new ArrayList<>();
        for (Lab lab : unassignedLabs) {
            unassignedList.add(new LabSummaryDto(lab));
        }
        dto.setLabsWithoutTechnician(unassignedList);

        // 3. Complaints grouped by Lab (includes active labs with 0 complaints)
        List<ChartDataDto> complaintsByLab = new ArrayList<>();
        for (Object[] row : labRepository.countComplaintsGroupedByActiveLab()) {
            String labName = (String) row[0];
            long count = ((Number) row[1]).longValue();
            complaintsByLab.add(new ChartDataDto(labName, count));
        }
        dto.setComplaintsByLab(complaintsByLab);

        // 4. Complaints grouped by Complaint Type
        List<ChartDataDto> complaintsByType = new ArrayList<>();
        for (Object[] row : complaintRepository.countComplaintsGroupByType()) {
            String typeName = row[0].toString();
            long count = ((Number) row[1]).longValue();
            complaintsByType.add(new ChartDataDto(typeName, count));
        }
        dto.setComplaintsByType(complaintsByType);

        // 5. Complaints grouped by Status
        List<ChartDataDto> complaintsByStatus = List.of(
                new ChartDataDto(ComplaintStatus.PENDING.name(), pendingComplaints),
                new ChartDataDto(ComplaintStatus.IN_PROGRESS.name(), inProgressComplaints),
                new ChartDataDto(ComplaintStatus.COMPLETED.name(), completedComplaints)
        );
        dto.setComplaintsByStatus(complaintsByStatus);

        return dto;
    }
}
