package com.patrick.wpb.cmt.ems.fi.controller;

import com.patrick.wpb.cmt.ems.fi.dto.RegionalAllocationDto;
import com.patrick.wpb.cmt.ems.fi.dto.RegionalAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.dto.StatusUpdateRequest;
import com.patrick.wpb.cmt.ems.fi.dto.SubmitRegionalAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.dto.TraderOrderSummaryDto;
import com.patrick.wpb.cmt.ems.fi.service.RegionalAllocationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RegionalAllocationController {

    private final RegionalAllocationService regionalAllocationService;

    @GetMapping("/regional-allocation")
    public ResponseEntity<List<TraderOrderSummaryDto>> getRegionalAllocationOrders() {
        List<TraderOrderSummaryDto> response = regionalAllocationService.fetchRegionalAllocationOrders().stream()
                .map(TraderOrderSummaryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{clientOrderId}/regional-allocation")
    public ResponseEntity<RegionalAllocationDto> upsertRegionalAllocation(@PathVariable String clientOrderId,
                                                                          @Valid @RequestBody RegionalAllocationRequest request) {
        return ResponseEntity.ok(
                RegionalAllocationDto.fromEntity(
                        regionalAllocationService.upsertAllocation(
                                clientOrderId,
                                request.getHkOrderQuantity(),
                                request.getSgOrderQuantity(),
                                request.getLimitValue(),
                                request.getLimitType(),
                                request.getSizeLimit()
                        )
                )
        );
    }

    @PostMapping("/{clientOrderId}/regional-allocation/submit")
    public ResponseEntity<TraderOrderSummaryDto> submitRegionalAllocation(@PathVariable String clientOrderId,
                                                                          @Valid @RequestBody SubmitRegionalAllocationRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        regionalAllocationService.submitForApproval(
                                clientOrderId,
                                request.getClientAllocationBreakdowns(),
                                request.getFinalPricedAllocationBreakdowns(),
                                request.getFinalRegionalAllocations(),
                                request.getChangedBy(),
                                request.getNote()
                        )
                )
        );
    }

    @PostMapping("/{clientOrderId}/regional-allocation/approve")
    public ResponseEntity<TraderOrderSummaryDto> approveRegionalAllocation(@PathVariable String clientOrderId,
                                                                           @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        regionalAllocationService.approve(clientOrderId, request.getChangedBy(), request.getNote())
                )
        );
    }

    @PostMapping("/{clientOrderId}/regional-allocation/reject")
    public ResponseEntity<TraderOrderSummaryDto> rejectRegionalAllocation(@PathVariable String clientOrderId,
                                                                          @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        regionalAllocationService.reject(clientOrderId, request.getChangedBy(), request.getNote())
                )
        );
    }
}

