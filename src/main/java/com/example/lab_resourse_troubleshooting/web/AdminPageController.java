package com.example.lab_resourse_troubleshooting.web;

import com.example.lab_resourse_troubleshooting.dto.CreateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.dto.LabRequestDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateTechnicianRequestDto;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.service.ComplaintService;
import com.example.lab_resourse_troubleshooting.service.DashboardService;
import com.example.lab_resourse_troubleshooting.service.LabService;
import com.example.lab_resourse_troubleshooting.service.TechnicianService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final DashboardService dashboardService;
    private final LabService labService;
    private final TechnicianService technicianService;
    private final ComplaintService complaintService;

    public AdminPageController(DashboardService dashboardService,
                               LabService labService,
                               TechnicianService technicianService,
                               ComplaintService complaintService) {
        this.dashboardService = dashboardService;
        this.labService = labService;
        this.technicianService = technicianService;
        this.complaintService = complaintService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        var summary = dashboardService.getDashboardSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("maxByLab", maxValue(summary.getComplaintsByLab()));
        model.addAttribute("maxByType", maxValue(summary.getComplaintsByType()));
        model.addAttribute("statusTotal", Math.max(1, summary.getTotalComplaints()));
        model.addAttribute("activeNav", "admin-dashboard");
        return "admin/dashboard";
    }

    private long maxValue(java.util.List<com.example.lab_resourse_troubleshooting.dto.ChartDataDto> data) {
        if (data == null || data.isEmpty()) {
            return 1;
        }
        return Math.max(1, data.stream().mapToLong(com.example.lab_resourse_troubleshooting.dto.ChartDataDto::getValue).max().orElse(0));
    }

    // ---------------------------------------------------------------- Labs

    @GetMapping("/labs")
    public String labs(Model model) {
        populateLabsModel(model);
        if (!model.containsAttribute("labForm")) {
            model.addAttribute("labForm", new LabRequestDto());
        }
        return "admin/labs";
    }

    @PostMapping("/labs")
    public String createLab(@Valid @ModelAttribute("labForm") LabRequestDto labForm,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            populateLabsModel(model);
            return "admin/labs";
        }
        try {
            labService.createLab(labForm);
            redirectAttributes.addFlashAttribute("successMessage", "Lab created successfully.");
            return "redirect:/admin/labs";
        } catch (RuntimeException ex) {
            populateLabsModel(model);
            model.addAttribute("createLabError", ex.getMessage());
            return "admin/labs";
        }
    }

    @PostMapping("/labs/{id}")
    public String updateLab(@PathVariable Long id,
                            @Valid @ModelAttribute("editLabForm") LabRequestDto editLabForm,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            populateLabsModel(model);
            model.addAttribute("editLabError", "Please review the lab details and try again.");
            model.addAttribute("editLabId", id);
            return "admin/labs";
        }
        try {
            labService.updateLab(id, editLabForm);
            redirectAttributes.addFlashAttribute("successMessage", "Lab updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/labs";
    }

    @PostMapping("/labs/{id}/assign")
    public String assignTechnician(@PathVariable Long id,
                                   @RequestParam(required = false) Long technicianId,
                                   RedirectAttributes redirectAttributes) {
        try {
            labService.assignTechnician(id, technicianId);
            redirectAttributes.addFlashAttribute("successMessage", "Technician assignment updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/labs";
    }

    @PostMapping("/labs/{id}/status")
    public String updateLabStatus(@PathVariable Long id,
                                  @RequestParam boolean active,
                                  RedirectAttributes redirectAttributes) {
        try {
            labService.updateLabStatus(id, active);
            redirectAttributes.addFlashAttribute("successMessage", active ? "Lab activated." : "Lab deactivated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/labs";
    }

    // ----------------------------------------------------------- Technicians

    @GetMapping("/technicians")
    public String technicians(Model model) {
        populateTechniciansModel(model);
        if (!model.containsAttribute("technicianForm")) {
            model.addAttribute("technicianForm", new CreateTechnicianRequestDto());
        }
        return "admin/technicians";
    }

    @PostMapping("/technicians")
    public String createTechnician(@Valid @ModelAttribute("technicianForm") CreateTechnicianRequestDto technicianForm,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            populateTechniciansModel(model);
            return "admin/technicians";
        }
        try {
            technicianService.createTechnician(technicianForm);
            redirectAttributes.addFlashAttribute("successMessage", "Technician account created successfully.");
            return "redirect:/admin/technicians";
        } catch (RuntimeException ex) {
            populateTechniciansModel(model);
            model.addAttribute("createTechnicianError", ex.getMessage());
            return "admin/technicians";
        }
    }

    @PostMapping("/technicians/{id}")
    public String updateTechnician(@PathVariable Long id,
                                   @ModelAttribute("editTechnicianForm") UpdateTechnicianRequestDto editTechnicianForm,
                                   RedirectAttributes redirectAttributes) {
        try {
            technicianService.updateTechnician(id, editTechnicianForm);
            redirectAttributes.addFlashAttribute("successMessage", "Technician updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/technicians";
    }

    @PostMapping("/technicians/{id}/status")
    public String updateTechnicianStatus(@PathVariable Long id,
                                         @RequestParam boolean active,
                                         RedirectAttributes redirectAttributes) {
        try {
            technicianService.updateTechnicianStatus(id, active);
            redirectAttributes.addFlashAttribute("successMessage", active ? "Technician activated." : "Technician deactivated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/technicians";
    }

    // ------------------------------------------------------------ Complaints

    @GetMapping("/complaints")
    public String complaints(@RequestParam(required = false) Long labId,
                             @RequestParam(required = false) ComplaintStatus status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("createdAt").descending());
        model.addAttribute("complaints", complaintService.getAllComplaintsForAdmin(labId, status, pageable));
        model.addAttribute("labs", labService.getAllLabs());
        model.addAttribute("statuses", ComplaintStatus.values());
        model.addAttribute("selectedLabId", labId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeNav", "admin-complaints");
        return "admin/complaints";
    }

    private void populateLabsModel(Model model) {
        model.addAttribute("labs", labService.getAllLabs());
        model.addAttribute("technicians", technicianService.getAllTechnicians());
        model.addAttribute("activeNav", "admin-labs");
    }

    private void populateTechniciansModel(Model model) {
        model.addAttribute("technicians", technicianService.getAllTechnicians());
        model.addAttribute("activeNav", "admin-technicians");
    }
}
