package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationAmendLogEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentAction;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentObjectType;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationAmendLogRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AmendLogService {

    private final ClientAllocationAmendLogRepository amendLogRepository;

    @Transactional
    public ClientAllocationAmendLogEntity recordAmendment(String refId,
                                                          AmendmentObjectType objectType,
                                                          String beforeJson,
                                                          String afterJson,
                                                          String createdBy) {
        int nextRevision = amendLogRepository.findFirstByRefIdOrderByRevisionDesc(refId)
                .map(log -> log.getRevision() + 1)
                .orElse(1);

        ClientAllocationAmendLogEntity logEntry = ClientAllocationAmendLogEntity.builder()
                .refId(refId)
                .revision(nextRevision)
                .objectType(objectType)
                .beforeObjectJson(beforeJson)
                .afterObjectJson(afterJson)
                .action(AmendmentAction.PENDING_APPROVAL)
                .createdBy(createdBy)
                .build();

        return amendLogRepository.save(logEntry);
    }

    @Transactional
    public void updateAction(String refId, AmendmentAction action) {
        ClientAllocationAmendLogEntity latestLog = amendLogRepository.findFirstByRefIdOrderByRevisionDesc(refId)
                .orElseThrow(() -> new IllegalStateException("No amend log found for refId " + refId));
        latestLog.setAction(action);
        amendLogRepository.save(latestLog);
    }

    @Transactional(readOnly = true)
    public Optional<ClientAllocationAmendLogEntity> findLatestByRefId(String refId) {
        return amendLogRepository.findFirstByRefIdOrderByRevisionDesc(refId);
    }
}

