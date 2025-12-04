package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.request.IPOExecRequest;
import com.patrick.wpb.cmt.ems.fi.request.RegionalCounterpartyExecutionRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPOExecutionService {

    private final TraderOrderRepository traderOrderRepository;

    /**
     * Executes IPO group order execution by creating new group orders for specified regions.
     * 
     * @param existingGroupOrderId The existing group order's client order ID
     * @param request The IPO execution request containing regional counterparty execution requests
     * @return List of newly created group orders
     */
    @Transactional
    public List<TraderOrderEntity> executeIPOOrder(String existingGroupOrderId, IPOExecRequest request) {
        // Validate and fetch existing group order
        TraderOrderEntity existingGroupOrder = traderOrderRepository.findById(existingGroupOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Group order not found for id: " + existingGroupOrderId));

        // Validate that it's a group order (should not have originalClientOrderId)
        if (existingGroupOrder.getOriginalClientOrderId() != null) {
            throw new IllegalStateException("Order with id " + existingGroupOrderId + " is not a group order");
        }

        // Get all child orders of the existing group order
        List<TraderOrderEntity> childOrders = traderOrderRepository.findByOriginalClientOrderId(existingGroupOrderId);
        
        if (childOrders.isEmpty()) {
            throw new IllegalStateException("No child orders found for group order: " + existingGroupOrderId);
        }

        List<TraderOrderEntity> newGroupOrders = new ArrayList<>();
        BigDecimal totalQuantityToDeduct = BigDecimal.ZERO;

        // Process each regional execution request
        for (RegionalCounterpartyExecutionRequest regionalRequest : request.getRegionalCounterpartyExecutionRequestList()) {
            String targetCountryCode = regionalRequest.getCountryCode();

            // Find child orders matching the target region
            List<TraderOrderEntity> matchingChildOrders = childOrders.stream()
                    .filter(order -> targetCountryCode.equals(order.getCountryCode()))
                    .collect(Collectors.toList());

            if (matchingChildOrders.isEmpty()) {
                log.warn("No child orders found for region {} in group order {}", targetCountryCode, existingGroupOrderId);
                continue;
            }

            // Calculate sum of quantities for this region
            BigDecimal regionalQuantity = matchingChildOrders.stream()
                    .map(TraderOrderEntity::getOrderQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create new group order for this region
            String newGroupOrderId = UUID.randomUUID().toString();
            TraderOrderEntity newGroupOrder = TraderOrderEntity.builder()
                    .clientOrderId(newGroupOrderId)
                    .tradeDate(existingGroupOrder.getTradeDate())
                    .countryCode(targetCountryCode) // New region
                    .status(IPOOrderStatus.NEW)
                    .subStatus(IPOOrderSubStatus.NONE)
                    .securityId(existingGroupOrder.getSecurityId())
                    .orderQuantity(regionalQuantity) // Sum of regional child order quantities
                    .cleanPrice(existingGroupOrder.getCleanPrice())
                    .originalClientOrderId(null) // This is a new group order
                    .build();

            TraderOrderEntity savedNewGroupOrder = traderOrderRepository.save(newGroupOrder);
            newGroupOrders.add(savedNewGroupOrder);

            // Update child orders' originalClientOrderId to point to new group order
            matchingChildOrders.forEach(childOrder -> {
                childOrder.setOriginalClientOrderId(newGroupOrderId);
            });
            traderOrderRepository.saveAll(matchingChildOrders);

            // Accumulate quantity to deduct from existing group order
            totalQuantityToDeduct = totalQuantityToDeduct.add(regionalQuantity);

            log.info("Created new group order {} for region {} with quantity {}", 
                    newGroupOrderId, targetCountryCode, regionalQuantity);
        }

        // Update existing group order quantity (exclude new group orders' quantities)
        BigDecimal newQuantity = existingGroupOrder.getOrderQuantity().subtract(totalQuantityToDeduct);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    String.format("Cannot deduct quantity %s from group order with quantity %s",
                            totalQuantityToDeduct, existingGroupOrder.getOrderQuantity()));
        }
        existingGroupOrder.setOrderQuantity(newQuantity);
        traderOrderRepository.save(existingGroupOrder);

        log.info("Updated existing group order {} quantity from {} to {}", 
                existingGroupOrderId, existingGroupOrder.getOrderQuantity().add(totalQuantityToDeduct), newQuantity);

        return newGroupOrders;
    }
}

