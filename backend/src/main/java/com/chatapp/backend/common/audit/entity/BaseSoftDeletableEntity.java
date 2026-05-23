package com.chatapp.backend.common.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Extends the audited entity with soft-delete columns. Use the {@code deleted_at}
 * filter at query time (or via a JPQL @Where clause on the entity) to exclude
 * soft-deleted rows.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseSoftDeletableEntity extends BaseAuditedEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted(UUID by) {
        this.deletedAt = Instant.now();
        this.deletedBy = by;
    }
}
