package com.moodysalem.hibernate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;

@MappedSuperclass
public class VersionedEntity extends BaseEntity {
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @JsonIgnore
    @Column(name = "created", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Long created;

    @JsonIgnore
    @Column(name = "updated", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Long updated;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Date getCreated() {
        return created != null ? new Date(created) : null;
    }

    public void setCreated(Date created) {
        this.created = created != null ? created.getTime() : null;
    }

    public Date getUpdated() {
        return updated != null ? new Date(updated) : null;
    }

    public void setUpdated(Date updated) {
        this.updated = updated != null ? updated.getTime() : null;
    }

    @PrePersist
    public void updateCreated() {
        setCreated(new Date());
        setUpdated(new Date());
    }

    @PreUpdate
    public void updateUpdated() {
        setUpdated(new Date());
    }
}
