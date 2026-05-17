package com.chatapp.backend.rbac.repository;
import com.chatapp.backend.rbac.entity.Permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
