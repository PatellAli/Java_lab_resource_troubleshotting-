package com.example.lab_resourse_troubleshooting.repository;

import com.example.lab_resourse_troubleshooting.entity.Role;
import com.example.lab_resourse_troubleshooting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findByRole(Role role);

    List<User> findByRoleAndActiveTrue(Role role);

    Optional<User> findByIdAndRole(Long id, Role role);
}
