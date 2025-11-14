package com.patrick.wpb.cmt.ems.fi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownDto;
import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationAmendLogEntity;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AllocationStatus;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentObjectType;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientAllocationService {

    private final TraderOrderRepository traderOrderRepository;
    private final ClientAllocationBreakdownRepository breakdownRepository;
    private final AmendLogService amendLogService;
    private final StatusService statusService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<TraderOrderEntity> fetchPendingClientAllocations() {
        return traderOrderRepository.findByStatusAndSubStatus(
                IPOOrderStatus.CLIENT_ALLOCATION,
                IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION);
    }

    @Transactional(readOnly = true)
    public List<TraderOrderEntity> fetchPendingClientAllocationApprovals() {
        return traderOrderRepository.findByStatusAndSubStatus(
                IPOOrderStatus.CLIENT_ALLOCATION,
                IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL);
    }

    @Transactional(readOnly = true)
    public List<ClientAllocationBreakdownEntity> getBreakdowns(String clientOrderId) {
        return breakdownRepository.findByOrderClientOrderId(clientOrderId);
    }

    @Transactional
    public List<ClientAllocationBreakdownEntity> saveDraftAllocations(String clientOrderId,
                                                                      List<ClientAllocationBreakdownRequest> items) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));
        if (order.getStatus() != IPOOrderStatus.CLIENT_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION) {
            throw new IllegalStateException("Draft allocations can only be saved when order is pending client allocation.");
        }

        List<ClientAllocationBreakdownEntity> existing = breakdownRepository.findByOrderClientOrderId(clientOrderId);
        if (!existing.isEmpty()) {
            breakdownRepository.deleteAll(existing);
        }

        List<ClientAllocationBreakdownEntity> newEntities = items.stream()
                .map(item -> mapToEntity(order, item))
                .collect(Collectors.toList());

        return breakdownRepository.saveAll(newEntities);
    }

    @Transactional
    public TraderOrderEntity submitForApproval(String clientOrderId,
                                               List<ClientAllocationBreakdownRequest> proposedBreakdowns,
                                               String changedBy,
                                               String note) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        if (order.getStatus() != IPOOrderStatus.CLIENT_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION) {
            throw new IllegalStateException("Order is not in pending client allocation status.");
        }

        validateFinalAllocations(order.getOrderQuantity(), proposedBreakdowns);

        String beforeJson = toJson(breakdownRepository.findByOrderClientOrderId(clientOrderId).stream()
                .map(ClientAllocationBreakdownDto::fromEntity)
                .toList());
        String afterJson = toJson(proposedBreakdowns);

        amendLogService.recordAmendment(
                clientOrderId,
                AmendmentObjectType.CLIENT_ALLOCATION_BREAKDOWN,
                beforeJson,
                afterJson,
                changedBy
        );

        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.CLIENT_ALLOCATION,
                IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL,
                changedBy,
                note
        );
    }

    @Transactional
    public TraderOrderEntity approve(String clientOrderId, String changedBy, String note) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        if (order.getStatus() != IPOOrderStatus.CLIENT_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL) {
            throw new IllegalStateException("Order is not pending client allocation approval.");
        }

        ClientAllocationAmendLogEntity latestLog = amendLogService.findLatestByRefId(clientOrderId)
                .orElseThrow(() -> new IllegalStateException("No client allocation amend log found for order " + clientOrderId));

        List<ClientAllocationBreakdownRequest> approvedBreakdowns = fromJson(latestLog.getAfterObjectJson());

        applyApprovedBreakdowns(order, approvedBreakdowns);

        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.CLIENT_ALLOCATION,
                IPOOrderSubStatus.DONE,
                changedBy,
                note
        );
    }

    private void applyApprovedBreakdowns(TraderOrderEntity order, List<ClientAllocationBreakdownRequest> approvedBreakdowns) {
        List<ClientAllocationBreakdownEntity> existing = breakdownRepository.findByOrderClientOrderId(order.getClientOrderId());
        if (!existing.isEmpty()) {
            breakdownRepository.deleteAll(existing);
        }

        List<ClientAllocationBreakdownEntity> entities = approvedBreakdowns.stream()
                .map(item -> {
                    ClientAllocationBreakdownEntity entity = mapToEntity(order, item);
                    // Set status to ACCEPTED when approving
                    entity.setAllocationStatus(AllocationStatus.ACCEPTED);
                    return entity;
                })
                .collect(Collectors.toList());
        breakdownRepository.saveAll(entities);
    }

    private ClientAllocationBreakdownEntity mapToEntity(TraderOrderEntity order, ClientAllocationBreakdownRequest dto) {
        return ClientAllocationBreakdownEntity.builder()
                .order(order)
                .countryCode(dto.getCountryCode())
                .accountNumber(dto.getAccountNumber())
                .orderQuantity(dto.getOrderQuantity())
                .finalAllocation(dto.getFinalAllocation())
                .allocationPercentage(dto.getAllocationPercentage())
                .estimatedOrderSize(dto.getEstimatedOrderSize())
                .yieldLimit(dto.getYieldLimit())
                .spreadLimit(dto.getSpreadLimit())
                .sizeLimit(dto.getSizeLimit())
                .build();
    }

    private void validateFinalAllocations(BigDecimal orderQuantity,
                                          List<ClientAllocationBreakdownRequest> breakdowns) {
        BigDecimal total = breakdowns.stream()
                .map(ClientAllocationBreakdownRequest::getFinalAllocation)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(orderQuantity) != 0) {
            throw new IllegalArgumentException("Final allocations must sum to the trader order quantity.");
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    private List<ClientAllocationBreakdownRequest> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ClientAllocationBreakdownRequest>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize client allocation breakdown JSON", e);
        }
    }
}

