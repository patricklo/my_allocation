package com.patrick.wpb.cmt.ems.fi.controller;

import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownDto;
import com.patrick.wpb.cmt.ems.fi.dto.StatusUpdateRequest;
import com.patrick.wpb.cmt.ems.fi.dto.SubmitClientAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.dto.TraderOrderSummaryDto;
import com.patrick.wpb.cmt.ems.fi.dto.UpdateClientAllocationsRequest;
import com.patrick.wpb.cmt.ems.fi.service.ClientAllocationService;
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
public class ClientAllocationController {

    private final ClientAllocationService clientAllocationService;

    @GetMapping("/client-allocation")
    public ResponseEntity<List<TraderOrderSummaryDto>> getPendingClientAllocationOrders() {
        List<TraderOrderSummaryDto> response = clientAllocationService.fetchPendingClientAllocations().stream()
                .map(TraderOrderSummaryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client-allocation/approvals")
    public ResponseEntity<List<TraderOrderSummaryDto>> getPendingClientAllocationApprovals() {
        List<TraderOrderSummaryDto> response = clientAllocationService.fetchPendingClientAllocationApprovals().stream()
                .map(TraderOrderSummaryDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clientOrderId}/client-allocations")
    public ResponseEntity<List<ClientAllocationBreakdownDto>> getClientAllocations(@PathVariable String clientOrderId) {
        List<ClientAllocationBreakdownDto> response = clientAllocationService.getBreakdowns(clientOrderId).stream()
                .map(ClientAllocationBreakdownDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{clientOrderId}/client-allocations")
    public ResponseEntity<List<ClientAllocationBreakdownDto>> saveDraftAllocations(@PathVariable String clientOrderId,
                                                                                   @Valid @RequestBody UpdateClientAllocationsRequest request) {
        return ResponseEntity.ok(
                clientAllocationService.saveDraftAllocations(clientOrderId, request.getBreakdowns()).stream()
                        .map(ClientAllocationBreakdownDto::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping("/{clientOrderId}/client-allocations/submit")
    public ResponseEntity<TraderOrderSummaryDto> submitForApproval(@PathVariable String clientOrderId,
                                                                   @Valid @RequestBody SubmitClientAllocationRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        clientAllocationService.submitForApproval(
                                clientOrderId,
                                request.getBreakdowns(),
                                request.getChangedBy(),
                                request.getNote()
                        )
                )
        );
    }

    @PostMapping("/{clientOrderId}/client-allocations/approve")
    public ResponseEntity<TraderOrderSummaryDto> approve(@PathVariable String clientOrderId,
                                                         @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        clientAllocationService.approve(clientOrderId, request.getChangedBy(), request.getNote())
                )
        );
    }

    @PostMapping("/{clientOrderId}/client-allocations/reject")
    public ResponseEntity<TraderOrderSummaryDto> reject(@PathVariable String clientOrderId,
                                                        @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                TraderOrderSummaryDto.fromEntity(
                        clientAllocationService.reject(clientOrderId, request.getChangedBy(), request.getNote())
                )
        );
    }
}

