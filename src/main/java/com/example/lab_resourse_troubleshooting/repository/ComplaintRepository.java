package com.example.lab_resourse_troubleshooting.repository;

import com.example.lab_resourse_troubleshooting.entity.Complaint;
import com.example.lab_resourse_troubleshooting.entity.ComplaintStatus;
import com.example.lab_resourse_troubleshooting.entity.ComplaintType;
import com.example.lab_resourse_troubleshooting.entity.Lab;
import com.example.lab_resourse_troubleshooting.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStudent(User student);

    List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Page<Complaint> findByStudentId(Long studentId, Pageable pageable);

    List<Complaint> findByTechnician(User technician);

    List<Complaint> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    Page<Complaint> findByTechnicianId(Long technicianId, Pageable pageable);

    List<Complaint> findByLab(Lab lab);

    Page<Complaint> findByLabId(Long labId, Pageable pageable);

    List<Complaint> findByStatus(ComplaintStatus status);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByLabIdAndStatus(Long labId, ComplaintStatus status, Pageable pageable);

    long countByStatus(ComplaintStatus status);

    long countByLab(Lab lab);

    long countByComplaintType(ComplaintType complaintType);

    @Query("SELECT c.lab.name, COUNT(c) FROM Complaint c GROUP BY c.lab.name")
    List<Object[]> countComplaintsGroupByLab();

    @Query("SELECT c.complaintType, COUNT(c) FROM Complaint c GROUP BY c.complaintType")
    List<Object[]> countComplaintsGroupByType();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countComplaintsGroupByStatus();
}
