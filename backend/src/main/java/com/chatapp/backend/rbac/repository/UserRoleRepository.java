package com.chatapp.backend.rbac.repository;
import com.chatapp.backend.rbac.entity.UserRoleId;
import com.chatapp.backend.rbac.entity.UserRole;
import com.chatapp.backend.rbac.entity.RolePermission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(UUID userId);

    /**
     * Returns the distinct permission codes granted to a user via every role they hold.
     * One query, joins through user_roles -> roles -> role_permissions -> permissions.
     */
    @Query("""
            select distinct p.code
              from UserRole ur
              join RolePermission rp on rp.role.id = ur.role.id
              join rp.permission p
             where ur.user.id = :userId
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndRoleCode(UUID userId, String roleCode);
}
