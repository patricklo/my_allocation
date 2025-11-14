package com.patrick.wpb.cmt.ems.fi.controller;

import com.patrick.wpb.cmt.ems.fi.dto.GroupOrdersRequest;
import com.patrick.wpb.cmt.ems.fi.dto.StatusUpdateRequest;
import com.patrick.wpb.cmt.ems.fi.dto.TraderOrderSummaryDto;
import com.patrick.wpb.cmt.ems.fi.service.TraderOrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCollectionController {

    private final TraderOrderService traderOrderService;

    @GetMapping("/collection")
    public ResponseEntity<List<TraderOrderSummaryDto>> getOrderCollection() {
        List<TraderOrderSummaryDto> response = traderOrderService.fetchOrderCollectionBlotter().stream()
                .map(TraderOrderSummaryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/group")
    public ResponseEntity<TraderOrderSummaryDto> groupOrders(@Valid @RequestBody GroupOrdersRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        traderOrderService.groupOrders(request.getClientOrderIds(), request.getCreatedBy())
                )
        );
    }

    @PostMapping("/{clientOrderId}/proceed-regional-allocation")
    public ResponseEntity<TraderOrderSummaryDto> proceedToRegionalAllocation(@PathVariable String clientOrderId,
                                                                             @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        traderOrderService.proceedToRegionalAllocation(clientOrderId, request.getChangedBy(), request.getNote())
                )
        );
    }
}

