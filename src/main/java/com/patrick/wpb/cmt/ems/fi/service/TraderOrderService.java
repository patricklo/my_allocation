package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.ClientAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.enums.RegionalAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.RegionalAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderSubOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TraderOrderService {

    private final TraderOrderRepository traderOrderRepository;
    private final TraderSubOrderRepository traderSubOrderRepository;
    private final RegionalAllocationBreakdownRepository regionalAllocationBreakdownRepository;
    private final ClientAllocationBreakdownRepository clientAllocationBreakdownRepository;
    private final StatusService statusService;

    @Transactional(readOnly = true)
    public List<TraderOrderEntity> fetchOrderCollectionBlotter() {
        return traderOrderRepository.findByStatusAndSubStatus(IPOOrderStatus.NEW, IPOOrderSubStatus.NONE)
                .stream()
                .filter(order -> traderSubOrderRepository.countByOrderClientOrderIdAndIssueIPOFlagTrue(order.getClientOrderId()) > 0)
                .collect(Collectors.toList());
    }

    @Transactional
    public TraderOrderEntity groupOrders(List<String> clientOrderIds, String createdBy) {
        if (clientOrderIds == null || clientOrderIds.size() < 2) {
            throw new IllegalArgumentException("At least two orders are required to perform grouping.");
        }

        List<TraderOrderEntity> orders = traderOrderRepository.findAllById(clientOrderIds);
        if (orders.size() != clientOrderIds.size()) {
            throw new IllegalArgumentException("One or more orders could not be found for grouping.");
        }

        TraderOrderEntity baseOrder = orders.get(0);
        IPOOrderStatus expectedStatus = IPOOrderStatus.NEW;
        IPOOrderSubStatus expectedSubStatus = IPOOrderSubStatus.NONE;
        LocalDate tradeDate = baseOrder.getTradeDate();
        String securityId = baseOrder.getSecurityId();

        boolean sameAttributes = orders.stream().allMatch(order ->
                order.getStatus() == expectedStatus
                        && order.getSubStatus() == expectedSubStatus
                        && tradeDate.equals(order.getTradeDate())
                        && securityId.equals(order.getSecurityId()));
        if (!sameAttributes) {
            throw new IllegalStateException("Orders must share trade date, security id, status, and sub status to be grouped.");
        }

        BigDecimal groupedQuantity = orders.stream()
                .map(TraderOrderEntity::getOrderQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String groupedClientOrderId = UUID.randomUUID().toString();

        TraderOrderEntity groupedOrder = TraderOrderEntity.builder()
                .clientOrderId(groupedClientOrderId)
                .tradeDate(tradeDate)
                .countryCode(baseOrder.getCountryCode())
                .status(IPOOrderStatus.NEW)
                .subStatus(IPOOrderSubStatus.NONE)
                .securityId(securityId)
                .orderQuantity(groupedQuantity)
                .cleanPrice(baseOrder.getCleanPrice())
                .build();

        TraderOrderEntity savedGroupedOrder = traderOrderRepository.save(groupedOrder);

        orders.forEach(order -> order.setOriginalClientOrderId(groupedClientOrderId));
        traderOrderRepository.saveAll(orders);

        return savedGroupedOrder;
    }

    @Transactional
    public TraderOrderEntity proceedToRegionalAllocation(String clientOrderId, String changedBy, String note) {
        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.REGIONAL_ALLOCATION,
                IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION,
                changedBy,
                note
        );
    }

    @Transactional
    public TraderOrderEntity ungroupOrder(String clientOrderId, String changedBy, String note) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        // Validate that order can be ungrouped (must be in REGIONAL_ALLOCATION or CLIENT_ALLOCATION status)
        if (order.getStatus() != IPOOrderStatus.REGIONAL_ALLOCATION && order.getStatus() != IPOOrderStatus.CLIENT_ALLOCATION) {
            throw new IllegalStateException("Order can only be ungrouped when in REGIONAL_ALLOCATION or CLIENT_ALLOCATION status");
        }

        // Mark Regional Allocation Breakdowns as INACTIVE
        var regionalBreakdowns = regionalAllocationBreakdownRepository.findByOrderClientOrderId(clientOrderId);
        regionalBreakdowns.forEach(breakdown -> breakdown.setRegionalAllocationStatus(RegionalAllocationStatus.INACTIVE));
        regionalAllocationBreakdownRepository.saveAll(regionalBreakdowns);

        // Mark Client Allocation Breakdowns as INACTIVE
        var clientBreakdowns = clientAllocationBreakdownRepository.findByOrderClientOrderId(clientOrderId);
        clientBreakdowns.forEach(breakdown -> breakdown.setClientAllocationStatus(ClientAllocationStatus.INACTIVE));
        clientAllocationBreakdownRepository.saveAll(clientBreakdowns);

        // Update order status to ACCEPTED with sub status NONE
        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.ACCEPTED,
                IPOOrderSubStatus.NONE,
                changedBy,
                note
        );
    }
}

