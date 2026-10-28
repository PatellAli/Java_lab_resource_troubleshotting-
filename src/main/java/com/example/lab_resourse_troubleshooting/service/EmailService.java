package com.example.lab_resourse_troubleshooting.service;

import com.example.lab_resourse_troubleshooting.entity.Complaint;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@labsystem.local}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendStatusChangeEmail(Complaint complaint, ComplaintStatus previousStatus, ComplaintStatus newStatus) {
        if (complaint.getStudent() == null || complaint.getStudent().getEmail() == null) {
            log.warn("Cannot send email: student email is missing for complaint #{}", complaint.getId());
            return;
        }

        String recipientEmail = complaint.getStudent().getEmail();
        String studentName = complaint.getStudent().getName() != null ? complaint.getStudent().getName() : "Student";
        String labName = complaint.getLab() != null ? complaint.getLab().getName() : "Unknown Lab";
        String note = complaint.getTechnicianNote() != null && !complaint.getTechnicianNote().isBlank()
                ? complaint.getTechnicianNote()
                : "None";
        String updateTime = complaint.getUpdatedAt() != null
                ? complaint.getUpdatedAt().format(FORMATTER)
                : "N/A";

        String subject = "[Lab Resource System] Complaint #" + complaint.getId() + " Status Updated: " + newStatus;

        String body = "Dear " + studentName + ",\n\n"
                + "The status of your submitted complaint has been updated.\n\n"
                + "Complaint Reference: #" + complaint.getId() + "\n"
                + "Lab: " + labName + "\n"
                + "Resource Number: " + complaint.getResourceNumber() + "\n"
                + "Complaint Type: " + complaint.getComplaintType() + "\n"
                + "Previous Status: " + previousStatus + "\n"
                + "New Status: " + newStatus + "\n"
                + "Technician Note: " + note + "\n"
                + "Updated At: " + updateTime + "\n\n"
                + "Problem Description:\n" + complaint.getDescription() + "\n\n"
                + "Regards,\nCollege Lab Resource Troubleshooting Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Status-change email sent for complaint #{} to {}", complaint.getId(), recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send email for complaint #{}: {}", complaint.getId(), ex.getMessage());
        }
    }
}
