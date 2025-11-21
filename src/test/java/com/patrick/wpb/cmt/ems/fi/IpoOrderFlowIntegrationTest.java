package com.patrick.wpb.cmt.ems.fi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalPricedAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalRegionalAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.dto.RegionalAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationAmendLogEntity;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalPricedAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalRegionalAllocationEntity;
import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderSubOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.AmendmentAction;
import com.patrick.wpb.cmt.ems.fi.enums.ClientAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.enums.RegionalAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationAmendLogRepository;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.RegionalAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalPricedAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalRegionalAllocationRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.service.ClientAllocationService;
import com.patrick.wpb.cmt.ems.fi.service.RegionalAllocationService;
import com.patrick.wpb.cmt.ems.fi.service.TraderOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class IpoOrderFlowIntegrationTest {

    @Autowired
    private TraderOrderRepository traderOrderRepository;

    @Autowired
    private ClientAllocationBreakdownRepository clientAllocationBreakdownRepository;

    @Autowired
    private ClientAllocationAmendLogRepository clientAllocationAmendLogRepository;

    @Autowired
    private RegionalAllocationBreakdownRepository regionalAllocationBreakdownRepository;

    @Autowired
    private FinalPricedAllocationBreakdownRepository finalPricedAllocationBreakdownRepository;

    @Autowired
    private FinalRegionalAllocationRepository finalRegionalAllocationRepository;

    @Autowired
    private TraderOrderService traderOrderService;

    @Autowired
    private RegionalAllocationService regionalAllocationService;

    @Autowired
    private ClientAllocationService clientAllocationService;

    @BeforeEach
    void setUp() {
        finalPricedAllocationBreakdownRepository.deleteAll();
        finalRegionalAllocationRepository.deleteAll();
        regionalAllocationBreakdownRepository.deleteAll();
        clientAllocationAmendLogRepository.deleteAll();
        clientAllocationBreakdownRepository.deleteAll();
        traderOrderRepository.deleteAll();

        traderOrderRepository.save(buildOrder("ORDER-1", "HK"));
        traderOrderRepository.save(buildOrder("ORDER-2", "SG"));
    }

    @Test
    @Transactional
    void endToEndFlow_succeeds() {
        List<TraderOrderEntity> collectionOrders = traderOrderService.fetchOrderCollectionBlotter();
        assertThat(collectionOrders).extracting(TraderOrderEntity::getClientOrderId)
                .containsExactlyInAnyOrder("ORDER-1", "ORDER-2");

        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1", "ORDER-2"), "test-user");
        assertThat(groupedOrder.getOrderQuantity()).isEqualByComparingTo("200");
        assertThat(groupedOrder.getStatus()).isEqualTo(IPOOrderStatus.NEW);

        TraderOrderEntity proceededOrder = traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "test-user", "Proceed to regional");
        assertThat(proceededOrder.getStatus()).isEqualTo(IPOOrderStatus.REGIONAL_ALLOCATION);
        assertThat(proceededOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);

        regionalAllocationService.upsertAllocation(
                groupedOrder.getClientOrderId(),
                new BigDecimal("120"),
                new BigDecimal("80"),
                new BigDecimal("1.50"),
                "YIELD",
                new BigDecimal("200")
        );

        // Prepare breakdown data for submission
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "120", "120"),
                buildRegionalAllocationRequest("SG", "ACCOUNT-SG", "80", "80")
        );
        
        List<FinalPricedAllocationBreakdownRequest> pricedBreakdowns = List.of(
                buildFinalPricedAllocationRequest("HK", "YIELD", new BigDecimal("99.5")),
                buildFinalPricedAllocationRequest("SG", "YIELD", new BigDecimal("99.5"))
        );
        
        List<FinalRegionalAllocationRequest> regionalAllocations = List.of(
                buildFinalRegionalAllocationRequest("ASIA", new BigDecimal("200"))
        );

        TraderOrderEntity pendingApproval = regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                pricedBreakdowns,
                regionalAllocations,
                "approver",
                "Submit regional allocation");
        assertThat(pendingApproval.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL);

        // Verify Regional Allocation Breakdowns are created with status=NEW
        List<RegionalAllocationBreakdownEntity> breakdownsAfterSubmit = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterSubmit).hasSize(2);
        assertThat(breakdownsAfterSubmit).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);

        // Verify Final Priced Allocation Breakdown is created
        List<FinalPricedAllocationBreakdownEntity> pricedBreakdownsResult = finalPricedAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(pricedBreakdownsResult).hasSize(2);

        // Verify Final Regional Allocation is created
        List<FinalRegionalAllocationEntity> finalRegionalList = finalRegionalAllocationRepository
                .findByClientOrderId(groupedOrder.getClientOrderId());
        assertThat(finalRegionalList).hasSize(1);
        assertThat(finalRegionalList.get(0).getAsiaAllocation()).isEqualByComparingTo("200");

        TraderOrderEntity clientAllocationPending = regionalAllocationService.approve(
                groupedOrder.getClientOrderId(), "approver", "Approve regional allocation");
        assertThat(clientAllocationPending.getStatus()).isEqualTo(IPOOrderStatus.CLIENT_ALLOCATION);
        assertThat(clientAllocationPending.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION);

        // Verify Regional Allocation Breakdowns are updated to ACCEPTED
        List<RegionalAllocationBreakdownEntity> breakdownsAfterApprove = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterApprove).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.ACCEPTED);

        List<ClientAllocationBreakdownRequest> draftBreakdowns = List.of(
                buildClientAllocationRequest("HK", "ACC-1", "120", "120"),
                buildClientAllocationRequest("SG", "ACC-2", "80", "80")
        );

        clientAllocationService.saveDraftAllocations(groupedOrder.getClientOrderId(), draftBreakdowns);
        assertThat(clientAllocationBreakdownRepository.findByOrderClientOrderId(groupedOrder.getClientOrderId()))
                .hasSize(2);

        TraderOrderEntity pendingClientApproval = clientAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(), draftBreakdowns, "allocator", "Submit client allocation");
        assertThat(pendingClientApproval.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL);

        // Verify amend log has action PENDING_APPROVAL after submission
        ClientAllocationAmendLogEntity amendLogAfterSubmit = clientAllocationAmendLogRepository
                .findFirstByRefIdOrderByRevisionDesc(groupedOrder.getClientOrderId()).orElseThrow();
        assertThat(amendLogAfterSubmit.getAction()).isEqualTo(AmendmentAction.PENDING_APPROVAL);

        TraderOrderEntity completedOrder = clientAllocationService.approve(
                groupedOrder.getClientOrderId(), "allocator", "Approve client allocation");
        assertThat(completedOrder.getStatus()).isEqualTo(IPOOrderStatus.CLIENT_ALLOCATION);
        assertThat(completedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.DONE);

        // Verify amend log has action APPROVED after approval
        ClientAllocationAmendLogEntity amendLogAfterApprove = clientAllocationAmendLogRepository
                .findFirstByRefIdOrderByRevisionDesc(groupedOrder.getClientOrderId()).orElseThrow();
        assertThat(amendLogAfterApprove.getAction()).isEqualTo(AmendmentAction.APPROVED);

        List<ClientAllocationBreakdownEntity> approvedBreakdowns =
                clientAllocationBreakdownRepository.findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(approvedBreakdowns)
                .hasSize(2)
                .extracting(ClientAllocationBreakdownEntity::getFinalAllocation)
                .containsExactlyInAnyOrder(new BigDecimal("120"), new BigDecimal("80"));
        assertThat(approvedBreakdowns).extracting(ClientAllocationBreakdownEntity::getClientAllocationStatus)
                .containsOnly(ClientAllocationStatus.ACCEPTED);
    }

    @Test
    @Transactional
    void rejectRegionalAllocation_revertsStatusAndBreakdowns() {
        TraderOrderEntity order1 = buildOrder("ORDER-1", "HK");
        traderOrderRepository.save(order1);

        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1"), "test-user");

        traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "test-user", "Proceed to regional");

        regionalAllocationService.upsertAllocation(
                groupedOrder.getClientOrderId(),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                new BigDecimal("1.50"),
                "YIELD",
                new BigDecimal("100")
        );

        // Prepare breakdown data for submission
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "100", "100")
        );
        
        List<FinalPricedAllocationBreakdownRequest> pricedBreakdowns = List.of(
                buildFinalPricedAllocationRequest("HK", "YIELD", new BigDecimal("99.5"))
        );
        
        List<FinalRegionalAllocationRequest> regionalAllocations = List.of(
                buildFinalRegionalAllocationRequest("ASIA", new BigDecimal("100"))
        );

        regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                pricedBreakdowns,
                regionalAllocations,
                "approver",
                "Submit regional allocation");

        // Verify Regional Allocation Breakdowns have status NEW
        List<RegionalAllocationBreakdownEntity> breakdownsBeforeReject = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsBeforeReject).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);

        // Reject
        TraderOrderEntity rejectedOrder = regionalAllocationService.reject(
                groupedOrder.getClientOrderId(), "approver", "Reject regional allocation");
        assertThat(rejectedOrder.getStatus()).isEqualTo(IPOOrderStatus.REGIONAL_ALLOCATION);
        assertThat(rejectedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);

        // Verify Regional Allocation Breakdowns still have status NEW (not changed on reject, but order status reverted)
        List<RegionalAllocationBreakdownEntity> breakdownsAfterReject = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterReject).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);
    }

    @Test
    @Transactional
    void rejectClientAllocation_revertsStatusAndBreakdowns() {
        TraderOrderEntity order1 = buildOrder("ORDER-1", "HK");
        traderOrderRepository.save(order1);

        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1"), "test-user");

        traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "test-user", "Proceed to regional");

        regionalAllocationService.upsertAllocation(
                groupedOrder.getClientOrderId(),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                new BigDecimal("1.50"),
                "YIELD",
                new BigDecimal("100")
        );

        // Prepare breakdown data for regional allocation submission
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "100", "100")
        );
        
        List<FinalPricedAllocationBreakdownRequest> pricedBreakdowns = List.of(
                buildFinalPricedAllocationRequest("HK", "YIELD", new BigDecimal("99.5"))
        );
        
        List<FinalRegionalAllocationRequest> regionalAllocations = List.of(
                buildFinalRegionalAllocationRequest("ASIA", new BigDecimal("100"))
        );

        regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                pricedBreakdowns,
                regionalAllocations,
                "approver",
                "Submit regional allocation");

        regionalAllocationService.approve(
                groupedOrder.getClientOrderId(), "approver", "Approve regional allocation");

        // Submit client allocation for approval
        List<ClientAllocationBreakdownRequest> clientBreakdowns = List.of(
                buildClientAllocationRequest("HK", "ACCOUNT-HK", "100", "100")
        );

        clientAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                clientBreakdowns,
                "approver",
                "Submit client allocation");

        // Verify breakdowns have status NEW after submission
        List<ClientAllocationBreakdownEntity> breakdownsBeforeReject = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsBeforeReject).extracting(ClientAllocationBreakdownEntity::getClientAllocationStatus)
                .containsOnly(ClientAllocationStatus.NEW);

        // Verify amend log has action PENDING_APPROVAL after submission
        ClientAllocationAmendLogEntity amendLogBeforeReject = clientAllocationAmendLogRepository
                .findFirstByRefIdOrderByRevisionDesc(groupedOrder.getClientOrderId()).orElseThrow();
        assertThat(amendLogBeforeReject.getAction()).isEqualTo(AmendmentAction.PENDING_APPROVAL);

        // Reject
        TraderOrderEntity rejectedOrder = clientAllocationService.reject(
                groupedOrder.getClientOrderId(), "approver", "Reject client allocation");
        assertThat(rejectedOrder.getStatus()).isEqualTo(IPOOrderStatus.CLIENT_ALLOCATION);
        assertThat(rejectedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION);

        // Verify Client Allocation Breakdowns still have status NEW (not changed on reject, but order status reverted)
        List<ClientAllocationBreakdownEntity> breakdownsAfterReject = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterReject).extracting(ClientAllocationBreakdownEntity::getClientAllocationStatus)
                .containsOnly(ClientAllocationStatus.NEW);

        // Verify amend log has action REJECTED after reject
        ClientAllocationAmendLogEntity amendLogAfterReject = clientAllocationAmendLogRepository
                .findFirstByRefIdOrderByRevisionDesc(groupedOrder.getClientOrderId()).orElseThrow();
        assertThat(amendLogAfterReject.getAction()).isEqualTo(AmendmentAction.REJECTED);
    }

    @Test
    @Transactional
    void ungroupOrder_fromRegionalAllocation_succeeds() {
        // Setup: Create a grouped order and proceed to regional allocation
        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1", "ORDER-2"), "user123");
        
        traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "user123", "Proceed to regional allocation");
        
        // Create some regional allocation breakdowns
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "120", "120"),
                buildRegionalAllocationRequest("SG", "ACCOUNT-SG", "80", "80")
        );
        
        regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                List.of(),
                List.of(),
                "user123",
                "Submit regional allocation");
        
        // Verify breakdowns exist and are NEW
        List<RegionalAllocationBreakdownEntity> breakdownsBeforeUngroup = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsBeforeUngroup).hasSize(2);
        assertThat(breakdownsBeforeUngroup).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);
        
        // Ungroup the order
        TraderOrderEntity ungroupedOrder = traderOrderService.ungroupOrder(
                groupedOrder.getClientOrderId(), "user123", "Ungroup order");
        
        // Verify order status changed to ACCEPTED/NONE
        assertThat(ungroupedOrder.getStatus()).isEqualTo(IPOOrderStatus.ACCEPTED);
        assertThat(ungroupedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);
        
        // Verify regional allocation breakdowns are marked as INACTIVE
        List<RegionalAllocationBreakdownEntity> breakdownsAfterUngroup = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterUngroup).hasSize(2);
        assertThat(breakdownsAfterUngroup).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.INACTIVE);
    }

    @Test
    @Transactional
    void ungroupOrder_fromClientAllocation_succeeds() {
        // Setup: Create a grouped order and proceed through to client allocation
        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1", "ORDER-2"), "user123");
        
        // Proceed through regional allocation
        traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "user123", "Proceed to regional allocation");
        
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "120", "120")
        );
        
        regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(), regionalBreakdowns, List.of(), List.of(),
                "user123", "Submit regional allocation");
        
        regionalAllocationService.approve(
                groupedOrder.getClientOrderId(), "approver", "Approve regional allocation");
        
        // Create client allocation breakdowns
        List<ClientAllocationBreakdownRequest> clientBreakdowns = List.of(
                buildClientAllocationRequest("HK", "ACCOUNT-HK", "110", "110")
        );
        
        clientAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(), clientBreakdowns, "user123", "Submit client allocation");
        
        // Verify client allocation breakdowns exist
        List<ClientAllocationBreakdownEntity> clientBreakdownsBeforeUngroup = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(clientBreakdownsBeforeUngroup).hasSize(1);
        assertThat(clientBreakdownsBeforeUngroup.get(0).getClientAllocationStatus()).isEqualTo(ClientAllocationStatus.NEW);
        
        // Ungroup the order
        TraderOrderEntity ungroupedOrder = traderOrderService.ungroupOrder(
                groupedOrder.getClientOrderId(), "user123", "Ungroup order");
        
        // Verify order status changed to ACCEPTED/NONE
        assertThat(ungroupedOrder.getStatus()).isEqualTo(IPOOrderStatus.ACCEPTED);
        assertThat(ungroupedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);
        
        // Verify client allocation breakdowns are marked as INACTIVE
        List<ClientAllocationBreakdownEntity> clientBreakdownsAfterUngroup = clientAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(clientBreakdownsAfterUngroup).hasSize(1);
        assertThat(clientBreakdownsAfterUngroup.get(0).getClientAllocationStatus()).isEqualTo(ClientAllocationStatus.INACTIVE);
    }

    @Test
    @Transactional
    void ungroupOrder_invalidStatus_throwsException() {
        // Try to ungroup an order that's still in NEW status
        assertThatThrownBy(() -> traderOrderService.ungroupOrder("ORDER-1", "user123", "Invalid ungroup"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order can only be ungrouped when in REGIONAL_ALLOCATION or CLIENT_ALLOCATION status");
    }

    private TraderOrderEntity buildOrder(String clientOrderId, String countryCode) {
        TraderOrderEntity order = TraderOrderEntity.builder()
                .clientOrderId(clientOrderId)
                .tradeDate(LocalDate.of(2024, 12, 1))
                .countryCode(countryCode)
                .status(IPOOrderStatus.NEW)
                .subStatus(IPOOrderSubStatus.NONE)
                .securityId("BOND-123")
                .orderQuantity(new BigDecimal("100"))
                .cleanPrice(new BigDecimal("99.5"))
                .build();

        TraderSubOrderEntity subOrder = TraderSubOrderEntity.builder()
                .countryCode(countryCode)
                .order(order)
                .accountId("ACCOUNT-" + countryCode)
                .issueIPOFlag(Boolean.TRUE)
                .build();

        order.getSubOrders().add(subOrder);
        return order;
    }

    private RegionalAllocationBreakdownRequest buildRegionalAllocationRequest(String countryCode,
                                                                              String accountNumber,
                                                                              String orderQuantity,
                                                                              String finalAllocation) {
        RegionalAllocationBreakdownRequest request = new RegionalAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setAccountNumber(accountNumber);
        request.setOrderQuantity(new BigDecimal(orderQuantity));
        request.setFinalAllocation(new BigDecimal(finalAllocation));
        request.setAllocationPercentage(new BigDecimal("0.50"));
        request.setEstimatedOrderSize(new BigDecimal(orderQuantity));
        request.setYieldLimit(new BigDecimal("1.50"));
        request.setSpreadLimit(new BigDecimal("0.25"));
        request.setSizeLimit(new BigDecimal(orderQuantity));
        return request;
    }

    private ClientAllocationBreakdownRequest buildClientAllocationRequest(String countryCode,
                                                                          String accountNumber,
                                                                          String orderQuantity,
                                                                          String finalAllocation) {
        ClientAllocationBreakdownRequest request = new ClientAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setAccountNumber(accountNumber);
        request.setOrderQuantity(new BigDecimal(orderQuantity));
        request.setFinalAllocation(new BigDecimal(finalAllocation));
        request.setAllocationPercentage(new BigDecimal("0.50"));
        request.setEstimatedOrderSize(new BigDecimal(orderQuantity));
        request.setYieldLimit(new BigDecimal("1.50"));
        request.setSpreadLimit(new BigDecimal("0.25"));
        request.setSizeLimit(new BigDecimal(orderQuantity));
        return request;
    }

    private FinalPricedAllocationBreakdownRequest buildFinalPricedAllocationRequest(String countryCode,
                                                                                    String limitType,
                                                                                    BigDecimal finalPrice) {
        FinalPricedAllocationBreakdownRequest request = new FinalPricedAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setLimitType(limitType);
        request.setFinalPrice(finalPrice);
        return request;
    }

    private FinalRegionalAllocationRequest buildFinalRegionalAllocationRequest(String market,
                                                                               BigDecimal allocation) {
        FinalRegionalAllocationRequest request = new FinalRegionalAllocationRequest();
        request.setMarket(market);
        request.setAsiaAllocation(allocation);
        request.setAllocation(allocation);
        request.setEffectiveOrder(allocation);
        request.setProRata(new BigDecimal("1.0000"));
        request.setAllocationAmount(allocation);
        return request;
    }
}

