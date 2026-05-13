package com.malek.owner_service.Repositories;

import com.malek.owner_service.Entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
    boolean existsByUser_IdAndRole(Long userId, String role);
}
