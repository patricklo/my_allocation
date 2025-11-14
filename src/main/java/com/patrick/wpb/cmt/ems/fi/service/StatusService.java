package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderStatusAuditEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderStatusAuditRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final TraderOrderRepository traderOrderRepository;
    private final TraderOrderStatusAuditRepository statusAuditRepository;

    private static final List<StatusTransition> ALLOWED_TRANSITIONS = List.of(
            new StatusTransition(IPOOrderStatus.NEW, IPOOrderSubStatus.NONE, IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION),
            new StatusTransition(IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION, IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL),
            new StatusTransition(IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL, IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION),
            new StatusTransition(IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL, IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION),
            new StatusTransition(IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION, IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL),
            new StatusTransition(IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL, IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.DONE)
    );

    @Transactional
    public TraderOrderEntity updateStatus(String clientOrderId,
                                          IPOOrderStatus targetStatus,
                                          IPOOrderSubStatus targetSubStatus,
                                          String changedBy,
                                          String note) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        IPOOrderStatus currentStatus = order.getStatus();
        IPOOrderSubStatus currentSubStatus = order.getSubStatus();

        if (!isTransitionAllowed(currentStatus, currentSubStatus, targetStatus, targetSubStatus)) {
            throw new IllegalStateException(String.format(
                    "Transition from %s/%s to %s/%s is not allowed",
                    currentStatus, currentSubStatus, targetStatus, targetSubStatus));
        }

        order.setStatus(targetStatus);
        order.setSubStatus(targetSubStatus);
        TraderOrderEntity saved = traderOrderRepository.save(order);

        TraderOrderStatusAuditEntity auditEntry = TraderOrderStatusAuditEntity.builder()
                .order(saved)
                .fromStatus(currentStatus)
                .fromSubStatus(currentSubStatus)
                .toStatus(targetStatus)
                .toSubStatus(targetSubStatus)
                .changedBy(changedBy)
                .changedAt(Instant.now())
                .note(note)
                .build();
        statusAuditRepository.save(auditEntry);

        return saved;
    }

    private boolean isTransitionAllowed(IPOOrderStatus fromStatus,
                                        IPOOrderSubStatus fromSubStatus,
                                        IPOOrderStatus toStatus,
                                        IPOOrderSubStatus toSubStatus) {
        if (Objects.equals(fromStatus, toStatus) && Objects.equals(fromSubStatus, toSubStatus)) {
            return true;
        }
        return ALLOWED_TRANSITIONS.stream()
                .anyMatch(transition -> transition.matches(fromStatus, fromSubStatus, toStatus, toSubStatus));
    }

    private record StatusTransition(IPOOrderStatus fromStatus,
                                    IPOOrderSubStatus fromSubStatus,
                                    IPOOrderStatus toStatus,
                                    IPOOrderSubStatus toSubStatus) {

        private boolean matches(IPOOrderStatus currentStatus,
                                IPOOrderSubStatus currentSubStatus,
                                IPOOrderStatus targetStatus,
                                IPOOrderSubStatus targetSubStatus) {
            return fromStatus == currentStatus
                    && fromSubStatus == currentSubStatus
                    && toStatus == targetStatus
                    && toSubStatus == targetSubStatus;
        }
    }
}
