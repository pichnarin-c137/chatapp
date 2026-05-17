package com.chatapp.backend.rbac;

import com.chatapp.backend.common.audit.BaseAuditedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseAuditedEntity {

    /** Stable codes used in code and assertions. Keep in sync with V1__init.sql seeds. */
    public static final String CODE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String CODE_ADMIN       = "ADMIN";
    public static final String CODE_MODERATOR   = "MODERATOR";
    public static final String CODE_USER        = "USER";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean systemRole = false;
}
