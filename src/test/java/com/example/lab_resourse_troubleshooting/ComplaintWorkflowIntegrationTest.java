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
class ComplaintWorkflowIntegrationTest {

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

    private User student1;
    private User student2;
    private User tech1;
    private User tech2;
    private Lab labWithTech;
    private Lab labWithoutTech;
    private Lab inactiveLab;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        complaintRepository.deleteAll();
        labRepository.deleteAll();
        userRepository.deleteAll();

        student1 = userRepository.save(User.builder()
                .name("Student One")
                .email("student1@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        student2 = userRepository.save(User.builder()
                .name("Student Two")
                .email("student2@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.STUDENT)
                .active(true)
                .build());

        tech1 = userRepository.save(User.builder()
                .name("Tech One")
                .email("tech1@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        tech2 = userRepository.save(User.builder()
                .name("Tech Two")
                .email("tech2@lab.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.TECHNICIAN)
                .active(true)
                .build());

        labWithTech = labRepository.save(Lab.builder()
                .name("Software Lab 1")
                .location("Floor 2")
                .active(true)
                .assignedTechnician(tech1)
                .build());

        labWithoutTech = labRepository.save(Lab.builder()
                .name("Hardware Lab 2")
                .location("Floor 1")
                .active(true)
                .assignedTechnician(null)
                .build());

        inactiveLab = labRepository.save(Lab.builder()
                .name("Renovating Lab")
                .location("Floor 3")
                .active(false)
                .build());
    }

    @Test
    @WithMockUser(username = "student1@lab.com", roles = {"STUDENT"})
    void testStudentCreateComplaintWithAutomaticTechnicianCapture() throws Exception {
        String requestJson = "{" +
                "\"labId\":" + labWithTech.getId() + "," +
                "\"resourceNumber\":\"PC-15\"," +
                "\"complaintType\":\"KEYBOARD\"," +
                "\"description\":\"Keys are stuck\"" +
                "}";

        mockMvc.perform(post("/api/student/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studentId").value(student1.getId()))
                .andExpect(jsonPath("$.studentEmail").value("student1@lab.com"))
                .andExpect(jsonPath("$.labId").value(labWithTech.getId()))
                .andExpect(jsonPath("$.labName").value("Software Lab 1"))
                .andExpect(jsonPath("$.technicianId").value(tech1.getId()))
                .andExpect(jsonPath("$.technicianName").value("Tech One"))
                .andExpect(jsonPath("$.resourceNumber").value("PC-15"))
                .andExpect(jsonPath("$.complaintType").value("KEYBOARD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = "student1@lab.com", roles = {"STUDENT"})
    void testStudentCreateComplaintForLabWithoutTechnician() throws Exception {
        String requestJson = "{" +
                "\"labId\":" + labWithoutTech.getId() + "," +
                "\"resourceNumber\":\"PC-02\"," +
                "\"complaintType\":\"MONITOR\"," +
                "\"description\":\"Display flickers\"" +
                "}";

        mockMvc.perform(post("/api/student/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technicianId").doesNotExist())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "student1@lab.com", roles = {"STUDENT"})
    void testRejectComplaintForInactiveOrNonExistentLab() throws Exception {
        // Inactive lab
        String inactiveJson = "{" +
                "\"labId\":" + inactiveLab.getId() + "," +
                "\"resourceNumber\":\"PC-01\"," +
                "\"complaintType\":\"MOUSE\"," +
                "\"description\":\"Scroll wheel broken\"" +
                "}";

        mockMvc.perform(post("/api/student/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inactive")));

        // Non existent lab
        String nonExistentJson = "{" +
                "\"labId\":999999," +
                "\"resourceNumber\":\"PC-01\"," +
                "\"complaintType\":\"MOUSE\"," +
                "\"description\":\"Scroll wheel broken\"" +
                "}";

        mockMvc.perform(post("/api/student/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonExistentJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    @WithMockUser(username = "student1@lab.com", roles = {"STUDENT"})
    void testStudentViewOwnComplaintsAndDeniedOtherStudentComplaint() throws Exception {
        Complaint complaint1 = complaintRepository.save(Complaint.builder()
                .student(student1)
                .lab(labWithTech)
                .technician(tech1)
                .resourceNumber("PC-10")
                .complaintType(ComplaintType.MOUSE)
                .description("Not clicking")
                .status(ComplaintStatus.PENDING)
                .build());

        Complaint complaint2 = complaintRepository.save(Complaint.builder()
                .student(student2)
                .lab(labWithTech)
                .technician(tech1)
                .resourceNumber("PC-20")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Key missing")
                .status(ComplaintStatus.PENDING)
                .build());

        // Student 1 lists complaints: only sees complaint1
        mockMvc.perform(get("/api/student/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(complaint1.getId()));

        // Student 1 views own complaint
        mockMvc.perform(get("/api/student/complaints/" + complaint1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(complaint1.getId()));

        // Student 1 tries viewing Student 2's complaint -> 403 Forbidden
        mockMvc.perform(get("/api/student/complaints/" + complaint2.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tech1@lab.com", roles = {"TECHNICIAN"})
    void testTechnicianViewAssignedAndDeniedOtherTechnicianComplaints() throws Exception {
        Complaint assignedComplaint = complaintRepository.save(Complaint.builder()
                .student(student1)
                .lab(labWithTech)
                .technician(tech1)
                .resourceNumber("PC-11")
                .complaintType(ComplaintType.INTERNET)
                .description("No network")
                .status(ComplaintStatus.PENDING)
                .build());

        Complaint unassignedToTech1 = complaintRepository.save(Complaint.builder()
                .student(student1)
                .lab(labWithTech)
                .technician(tech2)
                .resourceNumber("PC-12")
                .complaintType(ComplaintType.POWER)
                .description("No power")
                .status(ComplaintStatus.PENDING)
                .build());

        // Tech 1 lists assigned complaints: only sees assignedComplaint
        mockMvc.perform(get("/api/technician/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(assignedComplaint.getId()));

        // Tech 1 views assigned complaint: OK
        mockMvc.perform(get("/api/technician/complaints/" + assignedComplaint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assignedComplaint.getId()));

        // Tech 1 tries accessing Tech 2's complaint -> 403 Forbidden
        mockMvc.perform(get("/api/technician/complaints/" + unassignedToTech1.getId()))
                .andExpect(status().isForbidden());

        // Tech 1 tries updating Tech 2's complaint -> 403 Forbidden
        String updateJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Attempting hijack\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + unassignedToTech1.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "tech1@lab.com", roles = {"TECHNICIAN"})
    void testTechnicianStatusTransitionsAndValidation() throws Exception {
        Complaint complaint = complaintRepository.save(Complaint.builder()
                .student(student1)
                .lab(labWithTech)
                .technician(tech1)
                .resourceNumber("PC-33")
                .complaintType(ComplaintType.SOFTWARE)
                .description("IDE crash")
                .status(ComplaintStatus.PENDING)
                .build());

        // 1. Valid transition: PENDING -> IN_PROGRESS
        String inProgressJson = "{\"status\":\"IN_PROGRESS\",\"technicianNote\":\"Investigating crash log\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inProgressJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.technicianNote").value("Investigating crash log"));

        // 2. Reject repeating the current status: IN_PROGRESS -> IN_PROGRESS
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inProgressJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already in status IN_PROGRESS")));

        // 3. Reject invalid transition back to PENDING: IN_PROGRESS -> PENDING
        String pendingJson = "{\"status\":\"PENDING\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pendingJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be moved back to PENDING")));

        // 4. Valid transition: IN_PROGRESS -> COMPLETED
        String completedJson = "{\"status\":\"COMPLETED\",\"technicianNote\":\"Reinstalled IDE. Works now.\"}";
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.technicianNote").value("Reinstalled IDE. Works now."));

        // 5. Reject updating COMPLETED complaint
        mockMvc.perform(patch("/api/technician/complaints/" + complaint.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inProgressJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Completed complaints cannot be modified")));
    }

    @Test
    @WithMockUser(username = "admin@lab.com", roles = {"ADMIN"})
    void testAdminCanViewAllComplaints() throws Exception {
        complaintRepository.save(Complaint.builder()
                .student(student1)
                .lab(labWithTech)
                .technician(tech1)
                .resourceNumber("PC-01")
                .complaintType(ComplaintType.MOUSE)
                .description("Issue 1")
                .status(ComplaintStatus.PENDING)
                .build());

        complaintRepository.save(Complaint.builder()
                .student(student2)
                .lab(labWithoutTech)
                .technician(null)
                .resourceNumber("PC-02")
                .complaintType(ComplaintType.KEYBOARD)
                .description("Issue 2")
                .status(ComplaintStatus.IN_PROGRESS)
                .build());

        mockMvc.perform(get("/api/admin/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Filter by status
        mockMvc.perform(get("/api/admin/complaints?status=IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].resourceNumber").value("PC-02"));
    }
}
