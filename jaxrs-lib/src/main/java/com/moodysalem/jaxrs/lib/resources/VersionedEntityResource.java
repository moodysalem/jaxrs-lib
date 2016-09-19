package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.VersionedEntity;
import com.moodysalem.jaxrs.lib.exceptionmappers.Error;
import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;

import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class VersionedEntityResource<T extends VersionedEntity> extends EntityResource<T> {
    @Override
    protected void verifyCanMergeData(List<T> list, Map<UUID, T> oldData) {
        super.verifyCanMergeData(list, oldData);

        final List<Error> failedVersionCheck = new LinkedList<>();
        list.forEach(e -> {
            if (e.getId() == null) {
                return;
            }
            final T old = oldData.get(e.getId());
            if (old != null) {
                if (e.getVersion() != old.getVersion()) {
                    failedVersionCheck.add(
                            new Error(
                                    e.getId(),
                                    "version",
                                    "Version check failed"
                            )
                    );
                }
            }
        });

        if (!failedVersionCheck.isEmpty()) {
            throw new RequestProcessingException(Response.Status.CONFLICT,
                    failedVersionCheck.stream().toArray(Error[]::new));
        }
    }
}
