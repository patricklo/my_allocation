package com.patrick.wpb.cmt.ems.fi.service;

import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalPricedAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalRegionalAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalPricedAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalRegionalAllocationEntity;
import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AllocationStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalPricedAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalRegionalAllocationRepository;
import com.patrick.wpb.cmt.ems.fi.repo.RegionalAllocationRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionalAllocationService {

    private final RegionalAllocationRepository regionalAllocationRepository;
    private final TraderOrderRepository traderOrderRepository;
    private final ClientAllocationBreakdownRepository clientAllocationBreakdownRepository;
    private final FinalPricedAllocationBreakdownRepository finalPricedAllocationBreakdownRepository;
    private final FinalRegionalAllocationRepository finalRegionalAllocationRepository;
    private final StatusService statusService;

    @Transactional(readOnly = true)
    public List<TraderOrderEntity> fetchRegionalAllocationOrders() {
        return traderOrderRepository.findByStatus(IPOOrderStatus.REGIONAL_ALLOCATION);
    }

    @Transactional
    public RegionalAllocationEntity upsertAllocation(String clientOrderId,
                                                     BigDecimal hkOrderQuantity,
                                                     BigDecimal sgOrderQuantity,
                                                     BigDecimal limitValue,
                                                     String limitType,
                                                     BigDecimal sizeLimit) {
        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        validateAllocations(order.getOrderQuantity(), hkOrderQuantity, sgOrderQuantity);

        RegionalAllocationEntity allocation = regionalAllocationRepository.findById(clientOrderId)
                .orElse(RegionalAllocationEntity.builder()
                        .clientOrderId(clientOrderId)
                        .order(order)
                        .build());

        allocation.setOrderQuantity(order.getOrderQuantity());
        allocation.setHkOrderQuantity(hkOrderQuantity);
        allocation.setSgOrderQuantity(sgOrderQuantity);
        allocation.setLimitValue(limitValue);
        allocation.setLimitType(limitType);
        allocation.setSizeLimit(sizeLimit);

        return regionalAllocationRepository.save(allocation);
    }

    @Transactional
    public TraderOrderEntity submitForApproval(String clientOrderId,
                                               List<ClientAllocationBreakdownRequest> clientAllocationBreakdowns,
                                               List<FinalPricedAllocationBreakdownRequest> finalPricedAllocationBreakdowns,
                                               List<FinalRegionalAllocationRequest> finalRegionalAllocations,
                                               String changedBy,
                                               String note) {
        // Verify regional allocation exists
        regionalAllocationRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalStateException("Regional allocation must be saved before submission."));

        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        if (order.getStatus() != IPOOrderStatus.REGIONAL_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION) {
            throw new IllegalStateException("Order is not in pending regional allocation status.");
        }

        // Upsert Client Allocation Breakdown records with status=NEW from request
        upsertClientAllocationBreakdowns(order, clientAllocationBreakdowns);

        // Upsert Final Priced Allocation Breakdowns from request (one per country: HK, SG)
        upsertFinalPricedAllocationBreakdowns(order, finalPricedAllocationBreakdowns);

        // Upsert Final Regional Allocations from request (multiple per order, one per market)
        upsertFinalRegionalAllocations(order, finalRegionalAllocations);

        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.REGIONAL_ALLOCATION,
                IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL,
                changedBy,
                note
        );
    }

    @Transactional
    public TraderOrderEntity approve(String clientOrderId, String changedBy, String note) {
        regionalAllocationRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalStateException("Regional allocation must exist before approval."));

        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        if (order.getStatus() != IPOOrderStatus.REGIONAL_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL) {
            throw new IllegalStateException("Order is not pending regional allocation approval.");
        }

        // Update Client Allocation Breakdown status to ACCEPTED
        List<ClientAllocationBreakdownEntity> breakdowns = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(clientOrderId);
        breakdowns.forEach(breakdown -> breakdown.setAllocationStatus(AllocationStatus.ACCEPTED));
        clientAllocationBreakdownRepository.saveAll(breakdowns);

        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.CLIENT_ALLOCATION,
                IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION,
                changedBy,
                note
        );
    }

    @Transactional
    public TraderOrderEntity reject(String clientOrderId, String changedBy, String note) {
        regionalAllocationRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalStateException("Regional allocation must exist before rejection."));

        TraderOrderEntity order = traderOrderRepository.findById(clientOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Trader order not found for id " + clientOrderId));

        if (order.getStatus() != IPOOrderStatus.REGIONAL_ALLOCATION
                || order.getSubStatus() != IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL) {
            throw new IllegalStateException("Order is not pending regional allocation approval.");
        }

        // Update Client Allocation Breakdown status to NEW
        List<ClientAllocationBreakdownEntity> breakdowns = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(clientOrderId);
        breakdowns.forEach(breakdown -> breakdown.setAllocationStatus(AllocationStatus.NEW));
        clientAllocationBreakdownRepository.saveAll(breakdowns);

        return statusService.updateStatus(
                clientOrderId,
                IPOOrderStatus.REGIONAL_ALLOCATION,
                IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION,
                changedBy,
                note
        );
    }

    private void validateAllocations(BigDecimal orderQuantity,
                                     BigDecimal hkOrderQuantity,
                                     BigDecimal sgOrderQuantity) {
        if (hkOrderQuantity == null || sgOrderQuantity == null) {
            throw new IllegalArgumentException("HK and SG order quantities are required.");
        }
        BigDecimal total = hkOrderQuantity.add(sgOrderQuantity);
        if (total.compareTo(orderQuantity) > 0) {
            throw new IllegalArgumentException("Regional allocation exceeds order quantity.");
        }
    }

    private void upsertClientAllocationBreakdowns(TraderOrderEntity order, List<ClientAllocationBreakdownRequest> breakdownRequests) {
        // Get existing breakdowns for this order
        List<ClientAllocationBreakdownEntity> existingBreakdowns = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(order.getClientOrderId());
        
        // Create a map of existing breakdowns by account number and country code for quick lookup
        var existingMap = existingBreakdowns.stream()
                .collect(Collectors.toMap(
                        b -> b.getCountryCode() + "|" + b.getAccountNumber(),
                        b -> b,
                        (existing, replacement) -> existing
                ));

        // Upsert breakdowns from request
        for (ClientAllocationBreakdownRequest request : breakdownRequests) {
            String key = request.getCountryCode() + "|" + request.getAccountNumber();
            ClientAllocationBreakdownEntity breakdown = existingMap.get(key);
            
            if (breakdown == null) {
                breakdown = ClientAllocationBreakdownEntity.builder()
                        .order(order)
                        .countryCode(request.getCountryCode())
                        .accountNumber(request.getAccountNumber())
                        .build();
            }
            
            breakdown.setOrderQuantity(request.getOrderQuantity());
            breakdown.setFinalAllocation(request.getFinalAllocation());
            breakdown.setAllocationPercentage(request.getAllocationPercentage());
            breakdown.setEstimatedOrderSize(request.getEstimatedOrderSize());
            breakdown.setYieldLimit(request.getYieldLimit());
            breakdown.setSpreadLimit(request.getSpreadLimit());
            breakdown.setSizeLimit(request.getSizeLimit());
            breakdown.setAllocationStatus(AllocationStatus.NEW);
            
            clientAllocationBreakdownRepository.save(breakdown);
        }

        // Remove breakdowns that are no longer in the request
        List<String> requestKeys = breakdownRequests.stream()
                .map(b -> b.getCountryCode() + "|" + b.getAccountNumber())
                .collect(Collectors.toList());
        
        List<ClientAllocationBreakdownEntity> toDelete = existingBreakdowns.stream()
                .filter(b -> !requestKeys.contains(b.getCountryCode() + "|" + b.getAccountNumber()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            clientAllocationBreakdownRepository.deleteAll(toDelete);
        }
    }

    private void upsertFinalPricedAllocationBreakdowns(TraderOrderEntity order, List<FinalPricedAllocationBreakdownRequest> pricedBreakdownRequests) {
        // Get existing final priced allocation breakdowns
        List<FinalPricedAllocationBreakdownEntity> existingPriced = finalPricedAllocationBreakdownRepository
                .findByOrderClientOrderId(order.getClientOrderId());
        
        // Create a map of existing breakdowns by country code for quick lookup
        var existingMap = existingPriced.stream()
                .collect(Collectors.toMap(
                        FinalPricedAllocationBreakdownEntity::getCountryCode,
                        p -> p,
                        (existing, replacement) -> existing
                ));

        // Upsert entries from request (one per country: HK, SG)
        for (FinalPricedAllocationBreakdownRequest request : pricedBreakdownRequests) {
            FinalPricedAllocationBreakdownEntity priced = existingMap.get(request.getCountryCode());
            if (priced == null) {
                priced = FinalPricedAllocationBreakdownEntity.builder()
                        .order(order)
                        .countryCode(request.getCountryCode())
                        .build();
            }
            priced.setLimitType(request.getLimitType());
            priced.setFinalPrice(request.getFinalPrice());
            priced.setCountryCode(request.getCountryCode());
            finalPricedAllocationBreakdownRepository.save(priced);
        }

        // Remove entries that are no longer in the request
        List<String> requestCountryCodes = pricedBreakdownRequests.stream()
                .map(FinalPricedAllocationBreakdownRequest::getCountryCode)
                .collect(Collectors.toList());
        
        List<FinalPricedAllocationBreakdownEntity> toDelete = existingPriced.stream()
                .filter(p -> !requestCountryCodes.contains(p.getCountryCode()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            finalPricedAllocationBreakdownRepository.deleteAll(toDelete);
        }
    }

    private void upsertFinalRegionalAllocations(TraderOrderEntity order, List<FinalRegionalAllocationRequest> requests) {
        // Get existing final regional allocations
        List<FinalRegionalAllocationEntity> existing = finalRegionalAllocationRepository
                .findByClientOrderId(order.getClientOrderId());
        
        // Create a map of existing allocations by market for quick lookup
        var existingMap = existing.stream()
                .collect(Collectors.toMap(
                        FinalRegionalAllocationEntity::getMarket,
                        a -> a,
                        (existing1, replacement) -> existing1
                ));

        // Upsert entries from request (one per market)
        for (FinalRegionalAllocationRequest request : requests) {
            FinalRegionalAllocationEntity finalRegional = existingMap.get(request.getMarket());
            if (finalRegional == null) {
                finalRegional = FinalRegionalAllocationEntity.builder()
                        .clientOrderId(order.getClientOrderId())
                        .market(request.getMarket())
                        .build();
            }
            
            finalRegional.setAsiaAllocation(request.getAsiaAllocation());
            finalRegional.setAllocation(request.getAllocation());
            finalRegional.setMarket(request.getMarket());
            finalRegional.setEffectiveOrder(request.getEffectiveOrder());
            finalRegional.setProRata(request.getProRata());
            finalRegional.setAllocationAmount(request.getAllocationAmount());
            
            finalRegionalAllocationRepository.save(finalRegional);
        }

        // Remove entries that are no longer in the request
        List<String> requestMarkets = requests.stream()
                .map(FinalRegionalAllocationRequest::getMarket)
                .collect(Collectors.toList());
        
        List<FinalRegionalAllocationEntity> toDelete = existing.stream()
                .filter(a -> !requestMarkets.contains(a.getMarket()))
                .collect(Collectors.toList());
        
        if (!toDelete.isEmpty()) {
            finalRegionalAllocationRepository.deleteAll(toDelete);
        }
    }
}

