package com.example.lab_resourse_troubleshooting;

import com.example.lab_resourse_troubleshooting.entity.*;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AdminDashboardIntegrationTest {

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        complaintRepository.deleteAll();
        labRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@lab.com", roles = {"STUDENT"})
    void testStudentAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tech@lab.com", roles = {"TECHNICIAN"})
    void testTechnicianAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testEmptyDatasetDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints").value(0))
                .andExpect(jsonPath("$.pendingComplaints").value(0))
                .andExpect(jsonPath("$.inProgressComplaints").value(0))
                .andExpect(jsonPath("$.completedComplaints").value(0))
                .andExpect(jsonPath("$.labsWithoutTechnicianCount").value(0))
                .andExpect(jsonPath("$.complaintsByLab", hasSize(0)))
                .andExpect(jsonPath("$.complaintsByType", hasSize(0)))
                .andExpect(jsonPath("$.complaintsByStatus", hasSize(3)))
                .andExpect(jsonPath("$.complaintsByStatus[0].value").value(0))
                .andExpect(jsonPath("$.complaintsByStatus[1].value").value(0))
                .andExpect(jsonPath("$.complaintsByStatus[2].value").value(0))
                .andExpect(jsonPath("$.labsWithoutTechnician", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testMixedComplaintsAndGroupingAccuracy() throws Exception {
        // Users
        User student = userRepository.save(User.builder()
                .name("Alice")
                .email("alice@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        User activeTech = userRepository.save(User.builder()
                .name("Bob Active")
                .email("bob@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        User inactiveTech = userRepository.save(User.builder()
                .name("Charlie Inactive")
                .email("charlie@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(false)
                .build());

        // Labs
        Lab lab1 = labRepository.save(Lab.builder()
                .name("Lab 1 Software")
                .location("Wing A")
                .active(true)
                .assignedTechnician(activeTech)
                .build());

        Lab lab2 = labRepository.save(Lab.builder()
                .name("Lab 2 Hardware")
                .location("Wing B")
                .active(true)
                .assignedTechnician(null) // unassigned active lab
                .build());

        Lab lab3 = labRepository.save(Lab.builder()
                .name("Lab 3 Robotics")
                .location("Wing C")
                .active(true)
                .assignedTechnician(inactiveTech) // inactive technician assigned
                .build());

        labRepository.save(Lab.builder()
                .name("Lab 4 Renovating")
                .location("Wing D")
                .active(false) // inactive lab
                .assignedTechnician(null)
                .build());

        // Complaints (all on Lab 1, Lab 2 & 3 have 0 complaints)
        complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab1)
                .technician(activeTech)
                .resourceNumber("PC-01")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Broken key")
                .status(ComplaintStatus.PENDING)
                .build());

        complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab1)
                .technician(activeTech)
                .resourceNumber("PC-02")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Sticky key")
                .status(ComplaintStatus.IN_PROGRESS)
                .build());

        complaintRepository.save(Complaint.builder()
                .student(student)
                .lab(lab1)
                .technician(activeTech)
                .resourceNumber("PC-03")
                .complaintType(ComplaintType.MONITOR)
                .description("No display")
                .status(ComplaintStatus.COMPLETED)
                .build());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                // Summary counts
                .andExpect(jsonPath("$.totalComplaints").value(3))
                .andExpect(jsonPath("$.pendingComplaints").value(1))
                .andExpect(jsonPath("$.inProgressComplaints").value(1))
                .andExpect(jsonPath("$.completedComplaints").value(1))
                .andExpect(jsonPath("$.labsWithoutTechnicianCount").value(2)) // Lab 2 & Lab 3
                // Actionable unassigned labs list
                .andExpect(jsonPath("$.labsWithoutTechnician", hasSize(2)))
                // Per-lab grouping (includes active labs with 0 complaints)
                .andExpect(jsonPath("$.complaintsByLab", hasSize(3)))
                .andExpect(jsonPath("$.complaintsByLab[?(@.label == 'Lab 1 Software')].value").value(hasItem(3)))
                .andExpect(jsonPath("$.complaintsByLab[?(@.label == 'Lab 2 Hardware')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.complaintsByLab[?(@.label == 'Lab 3 Robotics')].value").value(hasItem(0)))
                // Per-type grouping
                .andExpect(jsonPath("$.complaintsByType", hasSize(2)))
                .andExpect(jsonPath("$.complaintsByType[?(@.label == 'KEYBOARD')].value").value(hasItem(2)))
                .andExpect(jsonPath("$.complaintsByType[?(@.label == 'MONITOR')].value").value(hasItem(1)))
                // Per-status grouping
                .andExpect(jsonPath("$.complaintsByStatus[?(@.label == 'PENDING')].value").value(hasItem(1)))
                .andExpect(jsonPath("$.complaintsByStatus[?(@.label == 'IN_PROGRESS')].value").value(hasItem(1)))
                .andExpect(jsonPath("$.complaintsByStatus[?(@.label == 'COMPLETED')].value").value(hasItem(1)));
    }
}
