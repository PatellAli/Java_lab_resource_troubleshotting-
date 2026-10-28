package com.example.lab_resourse_troubleshooting.web;

import com.example.lab_resourse_troubleshooting.dto.ComplaintResponseDto;
import com.example.lab_resourse_troubleshooting.dto.CreateComplaintRequestDto;
import com.example.lab_resourse_troubleshooting.entity.ComplaintType;
import com.example.lab_resourse_troubleshooting.service.ComplaintService;
import com.example.lab_resourse_troubleshooting.service.LabService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student")
public class StudentPageController {

    private final ComplaintService complaintService;
    private final LabService labService;

    public StudentPageController(ComplaintService complaintService, LabService labService) {
        this.complaintService = complaintService;
        this.labService = labService;
    }

    @GetMapping({"", "/", "/complaints"})
    public String complaints(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Authentication authentication,
                             Model model) {
        populatePage(authentication, page, size, model);
        if (!model.containsAttribute("complaintForm")) {
            model.addAttribute("complaintForm", new CreateComplaintRequestDto());
        }
        return "student/complaints";
    }

    @PostMapping("/complaints")
    public String createComplaint(@Valid @ModelAttribute("complaintForm") CreateComplaintRequestDto complaintForm,
                                  BindingResult bindingResult,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            populatePage(authentication, page, size, model);
            model.addAttribute("formHasErrors", true);
            return "student/complaints";
        }
        try {
            ComplaintResponseDto created = complaintService.createComplaint(authentication.getName(), complaintForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Complaint #" + created.getId() + " submitted successfully. It has been routed to "
                            + (created.getTechnicianName() != null ? created.getTechnicianName() : "the lab team") + ".");
            return "redirect:/student/complaints";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/complaints";
        }
    }

    private void populatePage(Authentication authentication, int page, int size, Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("createdAt").descending());
        model.addAttribute("complaints", complaintService.getStudentComplaints(authentication.getName(), pageable));
        model.addAttribute("labs", labService.getActiveLabs());
        model.addAttribute("complaintTypes", ComplaintType.values());
        model.addAttribute("activeNav", "student-complaints");
    }
}
