package com.moodysalem.hibernate.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 36, columnDefinition = "CHAR(36)")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        if (getId() == null) {
            setId(UUID.randomUUID());
        }
    }

    /**
     * Shorthand for comparing IDs
     *
     * @param other entity to check ID
     * @return true if the IDs match
     */
    public boolean idMatch(BaseEntity other) {
        return other != null && Objects.equals(getId(), other.getId());
    }
}
