package com.moodysalem.jaxrs.lib.resources.config;

public class ErrorMessageConfig {
    public static ErrorMessageConfig DEFAULT = new ErrorMessageConfig(
            "%1 with ID %2 not found.",
            "Not authorized to create %1.",
            "Not authorized to delete %1 with ID %2.",
            "%1 with ID %2 has since been edited."
    );

    private final String idNotFound, unauthorizedSave, unauthorizedDelete, versionConflict;

    public ErrorMessageConfig(String idNotFound, String unauthorizedSave, String unauthorizedDelete, String versionConflict) {
        this.idNotFound = idNotFound;
        this.unauthorizedSave = unauthorizedSave;
        this.unauthorizedDelete = unauthorizedDelete;
        this.versionConflict = versionConflict;
    }

    public String getIdNotFound() {
        return idNotFound;
    }

    public String getUnauthorizedSave() {
        return unauthorizedSave;
    }

    public String getUnauthorizedDelete() {
        return unauthorizedDelete;
    }

    public String getVersionConflict() {
        return versionConflict;
    }
}
