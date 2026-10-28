package com.example.lab_resourse_troubleshooting;

import com.example.lab_resourse_troubleshooting.entity.*;
import com.example.lab_resourse_troubleshooting.repository.ComplaintRepository;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminManagementIntegrationTest {

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

    private User activeTechnician;
    private User inactiveTechnician;
    private User studentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        complaintRepository.deleteAll();
        labRepository.deleteAll();
        userRepository.deleteAll();

        // Seed users
        activeTechnician = userRepository.save(User.builder()
                .name("Tech Alice")
                .email("alice.tech@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        inactiveTechnician = userRepository.save(User.builder()
                .name("Tech Bob (Inactive)")
                .email("bob.tech@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(false)
                .build());

        studentUser = userRepository.save(User.builder()
                .name("Student Charlie")
                .email("charlie.student@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build());
    }

    @Test
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/technicians"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "charlie.student@lab.com", roles = {"STUDENT"})
    void testStudentAccessDeniedToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/technicians"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice.tech@lab.com", roles = {"TECHNICIAN"})
    void testTechnicianAccessDeniedToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/technicians"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminCreateAndListLabs() throws Exception {
        String labJson = "{\"name\":\"Computer Lab A\",\"location\":\"Building 1, Room 101\",\"active\":true}";

        mockMvc.perform(post("/api/admin/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Computer Lab A"))
                .andExpect(jsonPath("$.location").value("Building 1, Room 101"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.assignedTechnicianId").doesNotExist());

        mockMvc.perform(get("/api/admin/labs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Computer Lab A"));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminPreventDuplicateLabName() throws Exception {
        labRepository.save(Lab.builder()
                .name("Computer Lab A")
                .location("Room 101")
                .active(true)
                .build());

        String duplicateJson = "{\"name\":\"Computer Lab A\",\"location\":\"Room 102\"}";

        mockMvc.perform(post("/api/admin/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminCreateAndListTechnicians() throws Exception {
        String techJson = "{\"name\":\"Tech Dave\",\"email\":\"dave.tech@lab.com\",\"password\":\"Password123\"}";

        mockMvc.perform(post("/api/admin/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(techJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Tech Dave"))
                .andExpect(jsonPath("$.email").value("dave.tech@lab.com"))
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Listing technicians
        mockMvc.perform(get("/api/admin/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))); // Alice, Bob, and newly created Dave
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminPreventDuplicateTechnicianEmail() throws Exception {
        String duplicateTechJson = "{\"name\":\"Tech Duplicate\",\"email\":\"alice.tech@lab.com\",\"password\":\"Password123\"}";

        mockMvc.perform(post("/api/admin/technicians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateTechJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAssignAndUnassignTechnicianToLab() throws Exception {
        Lab lab = labRepository.save(Lab.builder()
                .name("Physics Lab")
                .location("Block B")
                .active(true)
                .build());

        // Assign active technician
        String assignJson = "{\"technicianId\":" + activeTechnician.getId() + "}";
        mockMvc.perform(patch("/api/admin/labs/" + lab.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnicianId").value(activeTechnician.getId()))
                .andExpect(jsonPath("$.assignedTechnicianName").value("Tech Alice"));

        Lab updatedLab = labRepository.findById(lab.getId()).orElseThrow();
        assertEquals(activeTechnician.getId(), updatedLab.getAssignedTechnician().getId());

        // Unassign technician
        String unassignJson = "{\"technicianId\":null}";
        mockMvc.perform(patch("/api/admin/labs/" + lab.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnicianId").doesNotExist());

        Lab unassignedLab = labRepository.findById(lab.getId()).orElseThrow();
        assertNull(unassignedLab.getAssignedTechnician());
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testRejectAssigningNonTechnicianUser() throws Exception {
        Lab lab = labRepository.save(Lab.builder().name("Bio Lab").active(true).build());

        // Try assigning a STUDENT as technician
        String assignStudentJson = "{\"technicianId\":" + studentUser.getId() + "}";
        mockMvc.perform(patch("/api/admin/labs/" + lab.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignStudentJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is not a technician")));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testRejectAssigningInactiveTechnician() throws Exception {
        Lab lab = labRepository.save(Lab.builder().name("Chemistry Lab").active(true).build());

        // Try assigning inactive technician
        String assignInactiveJson = "{\"technicianId\":" + inactiveTechnician.getId() + "}";
        mockMvc.perform(patch("/api/admin/labs/" + lab.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignInactiveJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot assign inactive technician")));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testChangingLabTechnicianDoesNotModifyExistingComplaints() throws Exception {
        // Create Lab with Tech 1
        Lab lab = labRepository.save(Lab.builder()
                .name("Electronics Lab")
                .assignedTechnician(activeTechnician)
                .active(true)
                .build());

        // Create complaint assigned to Tech 1
        Complaint complaint = complaintRepository.save(Complaint.builder()
                .student(studentUser)
                .lab(lab)
                .technician(activeTechnician)
                .resourceNumber("PC-05")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Broken key")
                .status(ComplaintStatus.PENDING)
                .build());

        // Create Tech 2
        User tech2 = userRepository.save(User.builder()
                .name("Tech Eve")
                .email("eve.tech@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        // Reassign lab to Tech 2
        String reassignJson = "{\"technicianId\":" + tech2.getId() + "}";
        mockMvc.perform(patch("/api/admin/labs/" + lab.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnicianId").value(tech2.getId()));

        // Verify complaint assigned technician is still Tech 1 (unchanged)
        Complaint complaintAfterReassignment = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(activeTechnician.getId(), complaintAfterReassignment.getTechnician().getId(),
                "Existing complaint technician snapshot must not be altered when lab technician changes");
    }
}
