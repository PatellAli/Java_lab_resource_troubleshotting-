package com.example.lab_resourse_troubleshooting.web;

import com.example.lab_resourse_troubleshooting.dto.ComplaintResponseDto;
import com.example.lab_resourse_troubleshooting.dto.UpdateComplaintStatusRequestDto;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
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

import java.util.List;

@Controller
@RequestMapping("/technician")
public class TechnicianPageController {

    private final ComplaintService complaintService;

    public TechnicianPageController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping({"", "/", "/complaints"})
    public String complaints(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Authentication authentication,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("createdAt").descending());
        model.addAttribute("complaints", complaintService.getTechnicianComplaints(authentication.getName(), pageable));
        model.addAttribute("activeNav", "technician-complaints");
        return "technician/complaints";
    }

    @GetMapping("/complaints/{id}")
    public String complaintDetail(@PathVariable Long id,
                                  Authentication authentication,
                                  Model model) {
        ComplaintResponseDto complaint = complaintService.getTechnicianComplaintById(authentication.getName(), id);
        populateDetailModel(complaint, model);
        if (!model.containsAttribute("statusForm")) {
            UpdateComplaintStatusRequestDto statusForm = new UpdateComplaintStatusRequestDto();
            statusForm.setStatus(nextStatus(complaint.getStatus()));
            model.addAttribute("statusForm", statusForm);
        }
        return "technician/detail";
    }

    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @Valid @ModelAttribute("statusForm") UpdateComplaintStatusRequestDto statusForm,
                               BindingResult bindingResult,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            ComplaintResponseDto complaint = complaintService.getTechnicianComplaintById(authentication.getName(), id);
            populateDetailModel(complaint, model);
            model.addAttribute("formHasErrors", true);
            return "technician/detail";
        }
        try {
            complaintService.updateComplaintStatus(authentication.getName(), id, statusForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Complaint #" + id + " status updated to " + statusForm.getStatus() + ".");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/technician/complaints/" + id;
    }

    private void populateDetailModel(ComplaintResponseDto complaint, Model model) {
        model.addAttribute("complaint", complaint);
        model.addAttribute("statuses", List.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.COMPLETED));
        model.addAttribute("activeNav", "technician-complaints");
    }

    private ComplaintStatus nextStatus(ComplaintStatus current) {
        return current == ComplaintStatus.COMPLETED ? ComplaintStatus.COMPLETED : ComplaintStatus.IN_PROGRESS;
    }
}
