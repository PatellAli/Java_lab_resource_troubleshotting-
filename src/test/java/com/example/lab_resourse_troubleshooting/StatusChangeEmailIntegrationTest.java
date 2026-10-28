package com.example.lab_resourse_troubleshooting;

import com.example.lab_resourse_troubleshooting.entity.*;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(StatusChangeEmailIntegrationTest.TestMailConfig.class)
@Transactional
class StatusChangeEmailIntegrationTest {

    @TestConfiguration
    static class TestMailConfig {
        @Bean
        @Primary
        public JavaMailSender mockJavaMailSender() {
            return Mockito.mock(JavaMailSender.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabRepository labRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    private User student;
    private User technician;
    private Lab lab;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Mockito.reset(mailSender);
        complaintRepository.deleteAll();
        labRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(User.builder()
                .name("Alice Student")
                .email("alice.student@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        technician = userRepository.save(User.builder()
                .name("Bob Technician")
                .email("bob.tech@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        lab = labRepository.save(Lab.builder()
                .name("Computer Lab 1")
                .location("Wing A")
                .active(true)
                .assignedTechnician(technician)
                .build());
    }

    @Test
    @WithMockUser(username = "bob.tech@lab.com", roles = {"TECHNICIAN"})
    void testActualStatusChangeTriggersOneNotification() throws Exception {
        Complaint complaint = complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab)
                .technician(technician)
                .resourceNumber("PC-07")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Enter key broken")
                .status(ComplaintStatus.PENDING)
                .build());

        String updateJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Replacing key switches\"}";

        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Verify exactly one email was sent
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertArrayEquals(new String[]{"alice.student@lab.com"}, sentMessage.getTo());
        assertTrue(sentMessage.getSubject().contains("Complaint #" + complaint.getId()));
        assertTrue(sentMessage.getSubject().contains("IN_PROGRESS"));

        String text = sentMessage.getText();
        assertNotNull(text);
        assertTrue(text.contains("Alice Student"));
        assertTrue(text.contains("Computer Lab 1"));
        assertTrue(text.contains("PC-07"));
        assertTrue(text.contains("PENDING"));
        assertTrue(text.contains("IN_PROGRESS"));
        assertTrue(text.contains("Replacing key switches"));
    }

    @Test
    @WithMockUser(username = "bob.tech@lab.com", roles = {"TECHNICIAN"})
    void testUnchangedStatusTriggersZeroNotifications() throws Exception {
        Complaint complaint = complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab)
                .technician(technician)
                .resourceNumber("PC-07")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Enter key broken")
                .status(ComplaintStatus.IN_PROGRESS)
                .build());

        // Attempt repeating same status IN_PROGRESS
        String sameStatusJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Duplicate attempt\"}";

        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameStatusJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already in status IN_PROGRESS")));

        // Zero emails must be sent
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @WithMockUser(username = "bob.tech@lab.com", roles = {"TECHNICIAN"})
    void testMailFailureDoesNotRollbackOrLoseStatusUpdate() throws Exception {
        Complaint complaint = complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab)
                .technician(technician)
                .resourceNumber("PC-07")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Enter key broken")
                .status(ComplaintStatus.PENDING)
                .build());

        // Simulate SMTP failure
        doThrow(new MailSendException("SMTP connection refused: mail server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        String updateJson = "{\"status\":\"COMPLETED\",\"technicianNote\":\"Replaced keyboard\"}";

        // Request still succeeds with 200 OK
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Status change must still be committed to database
        Complaint persistedComplaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.COMPLETED, persistedComplaint.getStatus());
        assertEquals("Replaced keyboard", persistedComplaint.getTechnicianNote());
        assertNotNull(persistedComplaint.getUpdatedAt());
    }
}
