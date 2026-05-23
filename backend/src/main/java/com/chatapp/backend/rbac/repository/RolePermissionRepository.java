package com.chatapp.backend.rbac.repository;
import com.chatapp.backend.rbac.entity.RolePermissionId;
import com.chatapp.backend.rbac.entity.RolePermission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findByRoleId(UUID roleId);
}
