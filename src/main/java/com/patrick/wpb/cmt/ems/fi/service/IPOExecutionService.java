package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.entity.OrderExecutionDetailEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.repo.OrderExecutionDetailRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.request.IPOExecRequest;
import com.patrick.wpb.cmt.ems.fi.request.RegionalCounterpartyExecutionRequest;
import com.patrick.wpb.cmt.ems.fi.util.TraderOrderCloneUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IPOExecutionService {

    private final TraderOrderRepository traderOrderRepository;
    private final OrderExecutionDetailRepository orderExecutionDetailRepository;

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

        // Find existing execution detail for the group order (to copy execution info)
        Optional<OrderExecutionDetailEntity> existingExecutionDetail = 
                orderExecutionDetailRepository.findByClientOrderId(existingGroupOrderId);

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

            // Create new group order for this region using clone utility
            String newGroupOrderId = UUID.randomUUID().toString();
            TraderOrderEntity newGroupOrder = TraderOrderCloneUtil.clone(
                    existingGroupOrder,
                    newGroupOrderId,
                    targetCountryCode,
                    regionalQuantity
            );

            TraderOrderEntity savedNewGroupOrder = traderOrderRepository.save(newGroupOrder);
            newGroupOrders.add(savedNewGroupOrder);

            // Create new OrderExecutionDetailEntity for the new group order
            if (existingExecutionDetail.isPresent()) {
                OrderExecutionDetailEntity newExecutionDetail = createExecutionDetail(
                        existingExecutionDetail.get(),
                        newGroupOrderId,
                        regionalQuantity,
                        regionalRequest.getCounterpartyId(),
                        request.getBookingCenter()
                );
                orderExecutionDetailRepository.save(newExecutionDetail);
                log.info("Created execution detail {} for new group order {}", 
                        newExecutionDetail.getExecutionId(), newGroupOrderId);
            } else {
                log.warn("No existing execution detail found for group order {}, skipping execution detail creation", 
                        existingGroupOrderId);
            }

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

    /**
     * Creates a new OrderExecutionDetailEntity based on existing execution detail.
     * Copies all fields except executionId, clientOrderId, executedSize, and counterpartyCode.
     * 
     * @param source The source execution detail to copy from
     * @param newClientOrderId The new client order ID for the execution detail
     * @param executedSize The executed size (quantity) for the new execution detail
     * @param counterpartyCode Optional counterparty code override
     * @param bookingCenter Optional booking center override
     * @return New OrderExecutionDetailEntity
     */
    private OrderExecutionDetailEntity createExecutionDetail(OrderExecutionDetailEntity source,
                                                              String newClientOrderId,
                                                              BigDecimal executedSize,
                                                              String counterpartyCode,
                                                              String bookingCenter) {
        OrderExecutionDetailEntity newExecutionDetail = new OrderExecutionDetailEntity();
        
        // Copy all properties from source using BeanUtils
        BeanUtils.copyProperties(source, newExecutionDetail);
        
        // Override specific fields
        newExecutionDetail.setExecutionId(UUID.randomUUID().toString());
        newExecutionDetail.setClientOrderId(newClientOrderId);
        newExecutionDetail.setExecutedSize(executedSize);
        
        // Override counterparty code if provided
        if (counterpartyCode != null && !counterpartyCode.isEmpty()) {
            newExecutionDetail.setCounterpartyCode(counterpartyCode);
        }
        
        // Override booking center if provided
        if (bookingCenter != null && !bookingCenter.isEmpty()) {
            newExecutionDetail.setBookingCenter(bookingCenter);
        }
        
        return newExecutionDetail;
    }
}

