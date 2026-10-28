package com.example.lab_resourse_troubleshooting.repository;

import com.example.lab_resourse_troubleshooting.entity.Lab;
import com.example.lab_resourse_troubleshooting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {

    List<Lab> findByActiveTrue();

    List<Lab> findByAssignedTechnician(User technician);

    List<Lab> findByAssignedTechnicianId(Long technicianId);

    List<Lab> findByAssignedTechnicianIsNull();

    long countByAssignedTechnicianIsNull();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT l FROM Lab l LEFT JOIN l.assignedTechnician t WHERE l.active = true AND (t IS NULL OR t.active = false)")
    List<Lab> findActiveLabsWithoutActiveTechnician();

    @Query("SELECT l.name, COUNT(c) FROM Lab l LEFT JOIN Complaint c ON c.lab.id = l.id WHERE l.active = true GROUP BY l.id, l.name ORDER BY l.name")
    List<Object[]> countComplaintsGroupedByActiveLab();
}
