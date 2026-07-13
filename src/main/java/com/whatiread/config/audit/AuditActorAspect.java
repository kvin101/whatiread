package com.whatiread.config.audit;

import com.whatiread.identity.security.SecurityUtils;
import com.whatiread.shared.persistence.AuditActorEntity;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditActorAspect {

    @Before("execution(* org.springframework.data.repository.CrudRepository+.save(..)) && args(entity)")
    public void applyAuditActor(Object entity) {
        if (!(entity instanceof AuditActorEntity auditEntity)) {
            return;
        }
        Optional<UUID> actorId = SecurityUtils.currentUserIdOptional();
        if (actorId.isEmpty()) {
            return;
        }
        UUID userId = actorId.get();
        if (auditEntity.getCreatedBy() == null) {
            auditEntity.setCreatedBy(userId);
        }
        auditEntity.setUpdatedBy(userId);
    }
}
