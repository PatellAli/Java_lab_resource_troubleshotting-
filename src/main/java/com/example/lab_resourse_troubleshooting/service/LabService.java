package com.example.lab_resourse_troubleshooting.service;

import com.example.lab_resourse_troubleshooting.dto.LabRequestDto;
import com.example.lab_resourse_troubleshooting.dto.LabResponseDto;
import com.example.lab_resourse_troubleshooting.entity.Lab;
import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import com.example.lab_resourse_troubleshooting.repository.LabRepository;
import com.example.lab_resourse_troubleshooting.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LabService {

    private final LabRepository labRepository;
    private final UserRepository userRepository;

    public LabService(LabRepository labRepository, UserRepository userRepository) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LabResponseDto> getAllLabs() {
        List<LabResponseDto> list = new ArrayList<>();
        for (Lab lab : labRepository.findAll()) {
            list.add(new LabResponseDto(lab));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<LabResponseDto> getActiveLabs() {
        List<LabResponseDto> list = new ArrayList<>();
        for (Lab lab : labRepository.findByActiveTrue()) {
            list.add(new LabResponseDto(lab));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public LabResponseDto getLabById(Long id) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lab not found with id: " + id));
        return new LabResponseDto(lab);
    }

    @Transactional
    public LabResponseDto createLab(LabRequestDto request) {
        String trimmedName = request.getName().trim();
        if (labRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("A lab with name '" + trimmedName + "' already exists");
        }

        User technician = null;
        if (request.getAssignedTechnicianId() != null) {
            technician = validateAndGetTechnician(request.getAssignedTechnicianId());
        }

        Lab lab = new Lab();
        lab.setName(trimmedName);
        lab.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        lab.setActive(request.getActive() != null ? request.getActive() : true);
        lab.setAssignedTechnician(technician);

        Lab savedLab = labRepository.save(lab);
        return new LabResponseDto(savedLab);
    }

    @Transactional
    public LabResponseDto updateLab(Long id, LabRequestDto request) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lab not found with id: " + id));

        String trimmedName = request.getName().trim();
        if (labRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new IllegalArgumentException("Another lab with name '" + trimmedName + "' already exists");
        }

        lab.setName(trimmedName);
        lab.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        if (request.getActive() != null) {
            lab.setActive(request.getActive());
        }

        if (request.getAssignedTechnicianId() != null) {
            User technician = validateAndGetTechnician(request.getAssignedTechnicianId());
            lab.setAssignedTechnician(technician);
        } else {
            lab.setAssignedTechnician(null);
        }

        Lab savedLab = labRepository.save(lab);
        return new LabResponseDto(savedLab);
    }

    @Transactional
    public LabResponseDto assignTechnician(Long labId, Long technicianId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found with id: " + labId));

        if (technicianId != null) {
            User technician = validateAndGetTechnician(technicianId);
            lab.setAssignedTechnician(technician);
        } else {
            lab.setAssignedTechnician(null);
        }

        Lab savedLab = labRepository.save(lab);
        return new LabResponseDto(savedLab);
    }

    @Transactional
    public LabResponseDto updateLabStatus(Long labId, boolean active) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found with id: " + labId));

        lab.setActive(active);
        Lab savedLab = labRepository.save(lab);
        return new LabResponseDto(savedLab);
    }

    private User validateAndGetTechnician(Long technicianId) {
        User user = userRepository.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found with id: " + technicianId));

        if (user.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("User with id " + technicianId + " is not a technician (role: " + user.getRole() + ")");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Cannot assign inactive technician with id: " + technicianId);
        }

        return user;
    }
}
