package com.example.lab_resourse_troubleshooting;

import com.example.lab_resourse_troubleshooting.dto.*;
import com.example.lab_resourse_troubleshooting.entity.*;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(FullWorkflowEndToEndIntegrationTest.TestMailConfig.class)
@Transactional
class FullWorkflowEndToEndIntegrationTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Mockito.reset(mailSender);
        complaintRepository.deleteAll();
        labRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testCompleteLifecycleWorkflow() throws Exception {
        // Step 1: Admin provisions Technician 1
        String tech1Json = "{\"name\":\"Tech Bob\",\"email\":\"bob.tech@lab.com\",\"password\":\"TechPass123\"}";
        MvcResult tech1Result = mockMvc.perform(post("/api/admin/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tech1Json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andReturn();

        Long tech1Id = extractIdFromResponse(tech1Result);

        // Step 2: Admin provisions Technician 2
        String tech2Json = "{\"name\":\"Tech Charlie\",\"email\":\"charlie.tech@lab.com\",\"password\":\"TechPass123\"}";
        MvcResult tech2Result = mockMvc.perform(post("/api/admin/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tech2Json))
                .andExpect(status().isCreated())
                .andReturn();

        Long tech2Id = extractIdFromResponse(tech2Result);

        // Step 3: Admin creates Lab and assigns Technician 1
        String labJson = "{\"name\":\"Robotics Lab\",\"location\":\"Building C, Room 204\",\"active\":true,\"assignedTechnicianId\":" + tech1Id + "}";
        MvcResult labResult = mockMvc.perform(post("/api/admin/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedTechnicianId").value(tech1Id))
                .andReturn();

        Long labId = extractIdFromResponse(labResult);

        // Step 4: Student registers via public registration
        String registerJson = "{\"name\":\"Alice Student\",\"email\":\"alice.student@lab.com\",\"password\":\"StudentPass123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // Step 5: Student submits complaint
        String complaintJson = "{\"labId\":" + labId + ",\"resourceNumber\":\"ROBOT-01\",\"complaintType\":\"POWER\",\"description\":\"Power unit won't turn on\"}";
        MvcResult complaintResult = mockMvc.perform(post("/api/student/complaints")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice.student@lab.com").roles("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(complaintJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.technicianId").value(tech1Id))
                .andExpect(jsonPath("$.technicianName").value("Tech Bob"))
                .andReturn();

        Long complaintId = extractIdFromResponse(complaintResult);

        // Step 6: Admin changes lab's assigned technician to Technician 2
        String reassignJson = "{\"technicianId\":" + tech2Id + "}";
        mockMvc.perform(patch("/api/admin/labs/" + labId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnicianId").value(tech2Id));

        // Step 7: Verify historical snapshot isolation: existing complaint is STILL assigned to Technician 1
        Complaint complaintInDb = complaintRepository.findById(complaintId).orElseThrow();
        assertEquals(tech1Id, complaintInDb.getTechnician().getId(), "Historical complaint assignment must be preserved");

        // Step 8: Technician 2 cannot update complaint assigned to Technician 1
        String tech2UpdateJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Hijack attempt\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaintId + "/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("charlie.tech@lab.com").roles("TECHNICIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tech2UpdateJson))
                .andExpect(status().isForbidden());

        // Step 9: Technician 1 transitions complaint to IN_PROGRESS (triggers 1st email)
        String tech1InProgressJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Checking fuse circuit\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaintId + "/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob.tech@lab.com").roles("TECHNICIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tech1InProgressJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        // Step 10: Technician 1 transitions complaint to COMPLETED (triggers 2nd email)
        String tech1CompletedJson = "{\"status\":\"COMPLETED\",\"technicianNote\":\"Replaced fuse. All functional.\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaintId + "/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("bob.tech@lab.com").roles("TECHNICIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tech1CompletedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));

        // Step 11: Student views their own complaint and sees COMPLETED status and technician note
        mockMvc.perform(get("/api/student/complaints/" + complaintId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice.student@lab.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.technicianNote").value("Replaced fuse. All functional."));

        // Step 12: Admin dashboard reflects completed complaint and accurate metrics
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints").value(1))
                .andExpect(jsonPath("$.pendingComplaints").value(0))
                .andExpect(jsonPath("$.inProgressComplaints").value(0))
                .andExpect(jsonPath("$.completedComplaints").value(1))
                .andExpect(jsonPath("$.complaintsByLab[0].label").value("Robotics Lab"))
                .andExpect(jsonPath("$.complaintsByLab[0].value").value(1))
                .andExpect(jsonPath("$.complaintsByType[0].label").value("POWER"))
                .andExpect(jsonPath("$.complaintsByType[0].value").value(1));
    }

    private Long extractIdFromResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        // Extract id using simple regex to avoid Jackson package version variations
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new IllegalStateException("ID not found in response: " + json);
    }
}
