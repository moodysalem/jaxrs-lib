package com.moodysalem.hibernate.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@MappedSuperclass
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(generator = JSOGGenerator.class)
public class BaseEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @JsonIgnore
    @Column(name = "created", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @JsonIgnore
    @Column(name = "updated", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @PrePersist
    protected void onCreate() {
        created = new Date();
        updated = new Date();
        if (getId() == null) {
            setId(UUID.randomUUID());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updated = new Date();
    }

    /**
     * Shorthand for comparing IDs
     *
     * @param other entity to check ID
     * @return true if the IDs match
     */
    public boolean idMatch(BaseEntity other) {
        return other != null && other.getId() == getId();
    }
}
